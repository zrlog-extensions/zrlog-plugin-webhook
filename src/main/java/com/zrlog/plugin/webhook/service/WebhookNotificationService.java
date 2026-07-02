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
import com.zrlog.plugin.webhook.model.WebhookDeliveryResponse;
import com.zrlog.plugin.webhook.model.WebhookSendEnvelope;
import com.zrlog.plugin.webhook.model.WebhookSendRequest;
import com.zrlog.plugin.webhook.model.WebhookSendResult;

@Service("webhookService")
@Capability(
        key = "notification.webhook.send",
        type = "notification_channel",
        label = "发送 Webhook 通知",
        description = "通过已配置的 Webhook 目标推送系统通知。",
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
        WebhookSendRequest request = parseRequest(requestPacket);
        SendContext context = parseContext(request);
        WebhookDeliveryResponse response = new WebhookDeliveryResponse();
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
                    WebhookSendResult result = webhookDeliveryClient.send(ioSession, config, request);
                    status = result.getStatus();
                    error = result.getError();
                    response = WebhookDeliveryResponse.from(result);
                    response.setChannel(WebhookRepository.CHANNEL_WEBHOOK);
                    response.setTargetType(config.getTargetType());
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

    private SendContext parseContext(WebhookSendRequest request) {
        SendContext context = new SendContext();
        context.title = text(request.getTitle());
        context.content = text(request.getContent());
        context.source = request.sourceText();
        context.requestId = text(request.getRequestId());
        return context;
    }

    private WebhookSendRequest parseRequest(MsgPacket requestPacket) {
        String json = requestPacket.getDataStr();
        if (!notBlank(json)) {
            return new WebhookSendRequest();
        }
        try {
            if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())) {
                WebhookSendEnvelope envelope = gson.fromJson(json, WebhookSendEnvelope.class);
                if (envelope != null && envelope.getPayload() != null) {
                    return envelope.getPayload();
                }
            }
            WebhookSendRequest request = gson.fromJson(json, WebhookSendRequest.class);
            return request == null ? new WebhookSendRequest() : request;
        } catch (Exception e) {
            return new WebhookSendRequest();
        }
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void sendResponse(IOSession ioSession,
                              MsgPacket requestPacket,
                              WebhookDeliveryResponse response,
                              int status,
                              String error) {
        response.setStatus(status);
        response.setError(error);
        if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(status == 200);
            result.setData(response.toMap());
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
