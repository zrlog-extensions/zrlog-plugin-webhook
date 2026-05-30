package com.zrlog.plugin.webhook.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.CapabilityInvokeResult;
import com.zrlog.plugin.type.ActionType;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookSendResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("webhookService")
@Capability(
        key = "notification.webhook.send",
        type = "notification_channel",
        label = "发送 Webhook 通知",
        description = "通过已配置的 Webhook 目标推送标准通知。",
        exposure = {"notification"},
        channel = "webhook",
        timeoutSeconds = 30
)
public class WebhookNotificationService implements IPluginService {

    private static final WebhookRepository REPOSITORY = WebhookRepository.getInstance();
    private final Gson gson = new Gson();
    private final WebhookDeliveryClient webhookDeliveryClient = new WebhookDeliveryClient();

    @Override
    public void handle(IOSession ioSession, MsgPacket requestPacket) {
        Map<String, Object> rawRequestMap = parseMap(requestPacket.getDataStr());
        Map<String, Object> requestMap = payloadMap(requestPacket, rawRequestMap);
        SendContext context = parseContext(requestMap);
        Map<String, Object> response = new LinkedHashMap<>();
        int status = 200;
        String error = "";
        try {
            if (!notBlank(context.title) || !notBlank(context.content)) {
                status = 400;
                error = "missing title or content";
            } else {
                WebhookConfig config = REPOSITORY.readConfig(ioSession);
                if (!notBlank(config.getWebhookUrl())) {
                    status = 400;
                    error = "Webhook 插件未配置：Webhook 地址";
                } else {
                    WebhookSendResult result = webhookDeliveryClient.send(ioSession, config, requestMap);
                    status = result.getStatus();
                    error = result.getError();
                    response.put("responseBody", result.getResponseBody());
                    response.put("channel", WebhookRepository.CHANNEL_WEBHOOK);
                    response.put("targetType", config.getTargetType());
                    REPOSITORY.record(ioSession, WebhookRepository.DIRECTION_OUTBOUND, WebhookRepository.CHANNEL_WEBHOOK,
                            context.title, context.content, context.source, result.isSuccess(), status, error, context.requestId);
                    sendResponse(ioSession, requestPacket, response, result.isSuccess() ? 200 : status, error);
                    return;
                }
            }
        } catch (Exception e) {
            status = 500;
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        REPOSITORY.record(ioSession, WebhookRepository.DIRECTION_OUTBOUND, WebhookRepository.CHANNEL_WEBHOOK,
                context.title, context.content, context.source, false, status, error, context.requestId);
        sendResponse(ioSession, requestPacket, response, status, error);
    }

    private SendContext parseContext(Map<String, Object> requestMap) {
        SendContext context = new SendContext();
        context.title = firstString(requestMap.get("title"));
        context.content = firstString(requestMap.get("content"));
        context.source = source(requestMap);
        context.requestId = firstString(requestMap.get("requestId"));
        return context;
    }

    private Map<String, Object> payloadMap(MsgPacket requestPacket, Map<String, Object> rawRequestMap) {
        if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())
                && rawRequestMap != null && rawRequestMap.get("payload") instanceof Map) {
            return (Map<String, Object>) rawRequestMap.get("payload");
        }
        return rawRequestMap == null ? new HashMap<String, Object>() : rawRequestMap;
    }

    private Map<String, Object> parseMap(String json) {
        if (!notBlank(json)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> map = gson.fromJson(json, Map.class);
            return map == null ? new HashMap<String, Object>() : map;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String firstString(Object value) {
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private String source(Map<String, Object> requestMap) {
        String sourcePluginName = firstString(requestMap.get("sourcePluginName"));
        String notificationType = firstString(requestMap.get("notificationType"));
        if (!sourcePluginName.isEmpty() || !notificationType.isEmpty()) {
            return "通知:" + (sourcePluginName.isEmpty() ? notificationType : sourcePluginName);
        }
        return "服务调用";
    }

    private void sendResponse(IOSession ioSession,
                              MsgPacket requestPacket,
                              Map<String, Object> response,
                              int status,
                              String error) {
        response.put("status", status);
        if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(status == 200);
            result.setData(response);
            if (!result.isSuccess()) {
                result.setErrorMessage(error == null || error.trim().isEmpty() ? "send webhook failed" : error);
            }
            ioSession.sendJsonMsg(result, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                    result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
            return;
        }
        ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                status == 200 ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static class SendContext {
        private String title = "";
        private String content = "";
        private String source = "服务调用";
        private String requestId = "";
    }
}
