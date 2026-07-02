package com.zrlog.plugin.webhook.model;

public class WebhookSendEnvelope {

    private WebhookSendRequest payload;

    public WebhookSendRequest getPayload() {
        return payload;
    }

    public void setPayload(WebhookSendRequest payload) {
        this.payload = payload;
    }
}
