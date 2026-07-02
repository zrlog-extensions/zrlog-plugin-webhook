package com.zrlog.plugin.webhook.model;

public class WebhookConfigValues {

    private String webhookUrl;
    private String webhookTargetType;
    private String webhookSigningSecret;
    private String webhookIncomingToken;
    private String webhookTimeoutSeconds;
    private String webhookLogRetentionDays;
    private String feishuWebhookUrl;
    private String feishuSecret;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookTargetType() {
        return webhookTargetType;
    }

    public void setWebhookTargetType(String webhookTargetType) {
        this.webhookTargetType = webhookTargetType;
    }

    public String getWebhookSigningSecret() {
        return webhookSigningSecret;
    }

    public void setWebhookSigningSecret(String webhookSigningSecret) {
        this.webhookSigningSecret = webhookSigningSecret;
    }

    public String getWebhookIncomingToken() {
        return webhookIncomingToken;
    }

    public void setWebhookIncomingToken(String webhookIncomingToken) {
        this.webhookIncomingToken = webhookIncomingToken;
    }

    public String getWebhookTimeoutSeconds() {
        return webhookTimeoutSeconds;
    }

    public void setWebhookTimeoutSeconds(String webhookTimeoutSeconds) {
        this.webhookTimeoutSeconds = webhookTimeoutSeconds;
    }

    public String getWebhookLogRetentionDays() {
        return webhookLogRetentionDays;
    }

    public void setWebhookLogRetentionDays(String webhookLogRetentionDays) {
        this.webhookLogRetentionDays = webhookLogRetentionDays;
    }

    public String getFeishuWebhookUrl() {
        return feishuWebhookUrl;
    }

    public void setFeishuWebhookUrl(String feishuWebhookUrl) {
        this.feishuWebhookUrl = feishuWebhookUrl;
    }

    public String getFeishuSecret() {
        return feishuSecret;
    }

    public void setFeishuSecret(String feishuSecret) {
        this.feishuSecret = feishuSecret;
    }
}
