package com.zrlog.plugin.webhook.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.webhook.model.WebhookApiResponse;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookDeliveryResponse;
import com.zrlog.plugin.webhook.model.WebhookPageData;
import com.zrlog.plugin.webhook.model.WebhookRequestParams;
import com.zrlog.plugin.webhook.model.WebhookSendRequest;
import com.zrlog.plugin.webhook.model.WebhookSendResult;
import com.zrlog.plugin.webhook.service.WebhookDeliveryClient;
import com.zrlog.plugin.webhook.service.WebhookRepository;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebhookController {

    private static final WebhookRepository REPOSITORY = WebhookRepository.getInstance();

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final Gson gson = new Gson();
    private final WebhookDeliveryClient webhookDeliveryClient = new WebhookDeliveryClient();

    public WebhookController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void update() {
        WebhookConfig config = REPOSITORY.saveConfig(session, params());
        response(WebhookApiResponse.success(config));
    }

    public void index() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", requestInfo.isDarkMode() ? "dark" : "light");
        data.put("data", gson.toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    public void json() {
        response(pageData());
    }

    public void list() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        response(WebhookApiResponse.success(REPOSITORY.page(session, params(), config.getRetentionDays())));
    }

    public void testWebhook() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        String title = "ZrLog Webhook 测试";
        String content = "当你看到这条消息时，说明 Webhook 推送已经可以正常工作。";
        WebhookSendResult result;
        if (!notBlank(config.getWebhookUrl())) {
            result = new WebhookSendResult();
            result.setStatus(400);
            result.setError("Webhook 插件未配置：Webhook 地址");
        } else {
            WebhookSendRequest payload = new WebhookSendRequest();
            payload.setChannel(WebhookRepository.CHANNEL_WEBHOOK);
            payload.setTitle(title);
            payload.setContent(content);
            payload.setNotificationType("test");
            payload.setSourcePluginName("webhook");
            result = webhookDeliveryClient.send(session, config, payload);
        }
        REPOSITORY.record(session, WebhookRepository.DIRECTION_OUTBOUND, WebhookRepository.CHANNEL_WEBHOOK,
                title, content, "测试发送", result.isSuccess(), result.getStatus(), result.getError(), "");
        WebhookDeliveryResponse data = WebhookDeliveryResponse.from(result);
        response(result.isSuccess() ? WebhookApiResponse.success(data) : WebhookApiResponse.error(result.getError(), data));
    }

    public void incoming() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        WebhookRequestParams params = params();
        if (!authPassed(config, params)) {
            REPOSITORY.record(session, WebhookRepository.DIRECTION_INBOUND, WebhookRepository.CHANNEL_INCOMING,
                    "Webhook 请求", "未授权请求", "公开入口", false, 401, "Invalid token", "");
            response(WebhookApiResponse.error("Invalid token", new WebhookDeliveryResponse(401)));
            return;
        }
        String rawBody = bodyText();
        String title = firstNonBlank(params.getTitle(), "Webhook 消息");
        String content = firstNonBlank(params.getContent(),
                params.getText(),
                params.getMessage(),
                rawBody);
        if (!notBlank(content) && params.hasIncomingPayload()) {
            content = gson.toJson(params);
        }
        String source = firstNonBlank(params.getSource(), "公开 Webhook");
        String requestId = firstNonBlank(params.getRequestId());
        REPOSITORY.record(session, WebhookRepository.DIRECTION_INBOUND, WebhookRepository.CHANNEL_INCOMING,
                title, content, source, true, 200, "", requestId);
        response(WebhookApiResponse.success(new WebhookDeliveryResponse(200)));
    }

    private WebhookApiResponse<WebhookPageData> pageData() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        Map<String, Object> overview = REPOSITORY.overview(session, config.getRetentionDays());
        WebhookRequestParams firstPageParams = new WebhookRequestParams();
        firstPageParams.setPage("1");
        firstPageParams.setPageSize("10");
        WebhookPageData data = new WebhookPageData();
        data.setDark(requestInfo.isDarkMode());
        data.setColorPrimary(requestInfo.getAdminColorPrimary());
        data.setPlugin(session.getPlugin());
        data.setConfig(config);
        data.setSummary(overview.get("summary"));
        data.setTrend(overview.get("trend"));
        data.setLogs(REPOSITORY.page(session, firstPageParams, config.getRetentionDays()));
        data.setIncomingPath("/p/webhook/incoming");
        return WebhookApiResponse.success(data);
    }

    private WebhookRequestParams params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            WebhookRequestParams jsonBody = parseJsonBody(bodyText().trim());
            if (jsonBody != null) {
                return jsonBody;
            }
        }
        return WebhookRequestParams.fromParams(this::hasParam, this::paramObject);
    }

    private WebhookRequestParams parseJsonBody(String body) {
        try {
            WebhookRequestParams params = gson.fromJson(body, WebhookRequestParams.class);
            if (params != null) {
                return params;
            }
        } catch (Exception ignored) {
            // The host may pass a ByteBuffer backing array with trailing bytes.
        }
        int objectEnd = body.lastIndexOf('}');
        if (objectEnd < 0 || objectEnd == body.length() - 1) {
            return null;
        }
        try {
            WebhookRequestParams params = gson.fromJson(body.substring(0, objectEnd + 1), WebhookRequestParams.class);
            return params == null ? null : params;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasParam(String key) {
        return requestInfo.getParam() != null && requestInfo.getParam().containsKey(key);
    }

    private Object paramObject(String key) {
        if (!hasParam(key)) {
            return null;
        }
        String[] values = requestInfo.getParam().get(key);
        return values.length == 1 ? values[0] : values;
    }

    private String bodyText() {
        if (requestInfo.getRequestBody() == null || requestInfo.getRequestBody().length == 0) {
            return "";
        }
        byte[] bytes = requestInfo.getRequestBody();
        int length = bytes.length;
        while (length > 0 && bytes[length - 1] == 0) {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private boolean authPassed(WebhookConfig config, WebhookRequestParams params) {
        if (!notBlank(config.getIncomingToken())) {
            return false;
        }
        String token = firstNonBlank(bearerToken(), headerValue("X-Webhook-Token"), params.getToken());
        return config.getIncomingToken().equals(token);
    }

    private String bearerToken() {
        String value = headerValue("Authorization");
        if (!notBlank(value)) {
            return "";
        }
        String prefix = "Bearer ";
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value.substring(prefix.length()).trim();
        }
        return "";
    }

    private String headerValue(String key) {
        if (requestInfo.getHeader() == null) {
            return "";
        }
        String value = requestInfo.getHeader().get(key);
        if (notBlank(value)) {
            return value;
        }
        for (Map.Entry<String, String> entry : requestInfo.getHeader().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private void response(WebhookApiResponse<?> response) {
        session.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                response.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
