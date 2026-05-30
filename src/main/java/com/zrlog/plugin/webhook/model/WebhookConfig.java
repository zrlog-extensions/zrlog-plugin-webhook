package com.zrlog.plugin.webhook.model;

public class WebhookConfig {

    private String webhookUrl = "";
    private String targetType = "feishu";
    private String signingSecret = "";
    private String incomingToken = "";
    private int timeoutSeconds = 10;
    private int retentionDays = 30;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public String getIncomingToken() {
        return incomingToken;
    }

    public void setIncomingToken(String incomingToken) {
        this.incomingToken = incomingToken;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
