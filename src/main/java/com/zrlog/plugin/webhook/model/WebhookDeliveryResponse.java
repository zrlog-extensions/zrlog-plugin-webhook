package com.zrlog.plugin.webhook.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookDeliveryResponse {

    private int status;
    private String responseBody = "";
    private String error = "";
    private String channel = "";
    private String targetType = "";

    public WebhookDeliveryResponse() {
    }

    public WebhookDeliveryResponse(int status) {
        this.status = status;
    }

    public static WebhookDeliveryResponse from(WebhookSendResult result) {
        WebhookDeliveryResponse response = new WebhookDeliveryResponse();
        if (result == null) {
            return response;
        }
        response.setStatus(result.getStatus());
        response.setResponseBody(result.getResponseBody());
        response.setError(result.getError());
        return response;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("responseBody", responseBody);
        map.put("channel", channel);
        map.put("targetType", targetType);
        map.put("status", status);
        map.put("error", error);
        return map;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }
}
