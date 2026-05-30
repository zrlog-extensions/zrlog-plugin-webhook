package com.zrlog.plugin.webhook.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookSendResult;
import com.zrlog.plugin.webhook.service.WebhookDeliveryClient;
import com.zrlog.plugin.webhook.service.WebhookRepository;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
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
        response(successMap(config));
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
        response(successMap(REPOSITORY.page(session, params(), config.getRetentionDays())));
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
            Map<String, Object> payload = new HashMap<>();
            payload.put("channel", WebhookRepository.CHANNEL_WEBHOOK);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("notificationType", "test");
            payload.put("sourcePluginName", "webhook");
            result = webhookDeliveryClient.send(session, config, payload);
        }
        REPOSITORY.record(session, WebhookRepository.DIRECTION_OUTBOUND, WebhookRepository.CHANNEL_WEBHOOK,
                title, content, "测试发送", result.isSuccess(), result.getStatus(), result.getError(), "");
        Map<String, Object> data = new HashMap<>();
        data.put("status", result.getStatus());
        data.put("responseBody", result.getResponseBody());
        data.put("error", result.getError());
        response(result.isSuccess() ? successMap(data) : errorMap(result.getError(), data));
    }

    public void incoming() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        Map<String, Object> params = params();
        if (!authPassed(config, params)) {
            REPOSITORY.record(session, WebhookRepository.DIRECTION_INBOUND, WebhookRepository.CHANNEL_INCOMING,
                    "Webhook 请求", "未授权请求", "公开入口", false, 401, "Invalid token", "");
            Map<String, Object> data = new HashMap<>();
            data.put("status", 401);
            response(errorMap("Invalid token", data));
            return;
        }
        String rawBody = bodyText();
        String title = firstNonBlank(firstString(params.get("title")), "Webhook 消息");
        String content = firstNonBlank(firstString(params.get("content")),
                firstString(params.get("text")),
                firstString(params.get("message")),
                rawBody);
        if (!notBlank(content) && !params.isEmpty()) {
            content = gson.toJson(params);
        }
        String source = firstNonBlank(firstString(params.get("source")), "公开 Webhook");
        String requestId = firstString(params.get("requestId"));
        REPOSITORY.record(session, WebhookRepository.DIRECTION_INBOUND, WebhookRepository.CHANNEL_INCOMING,
                title, content, source, true, 200, "", requestId);
        Map<String, Object> data = new HashMap<>();
        data.put("status", 200);
        response(successMap(data));
    }

    private Map<String, Object> pageData() {
        WebhookConfig config = REPOSITORY.readConfig(session);
        Map<String, Object> overview = REPOSITORY.overview(session, config.getRetentionDays());
        Map<String, Object> firstPageParams = new HashMap<>();
        firstPageParams.put("page", "1");
        firstPageParams.put("pageSize", "10");
        Map<String, Object> data = new HashMap<>();
        data.put("dark", requestInfo.isDarkMode());
        data.put("colorPrimary", requestInfo.getAdminColorPrimary());
        data.put("plugin", session.getPlugin());
        data.put("config", config);
        data.put("summary", overview.get("summary"));
        data.put("trend", overview.get("trend"));
        data.put("logs", REPOSITORY.page(session, firstPageParams, config.getRetentionDays()));
        data.put("incomingPath", "/p/webhook/incoming");
        return successMap(data);
    }

    private Map<String, Object> params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            String body = bodyText();
            if (body.trim().startsWith("{")) {
                try {
                    Map<String, Object> map = gson.fromJson(body, Map.class);
                    if (map != null) {
                        return map;
                    }
                } catch (Exception ignored) {
                    // Fall through to the form parser. The host may pass a ByteBuffer backing array with trailing bytes.
                }
            }
            Map<String, Object> formBody = parseFormBody(body);
            if (!formBody.isEmpty()) {
                return formBody;
            }
        }
        if (requestInfo.getParam() == null) {
            return new HashMap<>();
        }
        return requestInfo.simpleParam();
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

    private Map<String, Object> parseFormBody(String body) {
        Map<String, Object> map = new HashMap<>();
        if (!notBlank(body) || !body.contains("=")) {
            return map;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            if (index < 0) {
                continue;
            }
            String key = decode(pair.substring(0, index));
            String value = decode(pair.substring(index + 1));
            if (notBlank(key)) {
                map.put(key, value);
            }
        }
        return map;
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private boolean authPassed(WebhookConfig config, Map<String, Object> params) {
        if (!notBlank(config.getIncomingToken())) {
            return false;
        }
        String token = firstNonBlank(bearerToken(), headerValue("X-Webhook-Token"), firstString(params.get("token")));
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

    private String firstString(Object value) {
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(ContentType.JSON, map, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                Boolean.FALSE.equals(map.get("success")) ? MsgPacketStatus.RESPONSE_ERROR : MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorMap(String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("message", message);
        map.put("data", data);
        return map;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
