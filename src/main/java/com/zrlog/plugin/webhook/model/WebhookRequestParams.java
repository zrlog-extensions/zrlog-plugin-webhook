package com.zrlog.plugin.webhook.model;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class WebhookRequestParams {

    private String webhookUrl;
    private String feishuWebhookUrl;
    private String webhookTargetType;
    private String targetType;
    private String webhookSigningSecret;
    private String feishuSecret;
    private String webhookIncomingToken;
    private String incomingToken;
    private String webhookTimeoutSeconds;
    private String timeoutSeconds;
    private String webhookLogRetentionDays;
    private String retentionDays;
    private String page;
    private String pageSize;
    private String keyword;
    private String status;
    private String direction;
    private String token;
    private String title;
    private String content;
    private String text;
    private String message;
    private String source;
    private String requestId;

    public static WebhookRequestParams fromParams(Predicate<String> hasParam, Function<String, Object> paramValue) {
        WebhookRequestParams request = new WebhookRequestParams();
        if (hasParam.test("webhookUrl")) {
            request.setWebhookUrl(stringValue(paramValue.apply("webhookUrl")));
        }
        if (hasParam.test("feishuWebhookUrl")) {
            request.setFeishuWebhookUrl(stringValue(paramValue.apply("feishuWebhookUrl")));
        }
        if (hasParam.test("webhookTargetType")) {
            request.setWebhookTargetType(stringValue(paramValue.apply("webhookTargetType")));
        }
        if (hasParam.test("targetType")) {
            request.setTargetType(stringValue(paramValue.apply("targetType")));
        }
        if (hasParam.test("webhookSigningSecret")) {
            request.setWebhookSigningSecret(stringValue(paramValue.apply("webhookSigningSecret")));
        }
        if (hasParam.test("feishuSecret")) {
            request.setFeishuSecret(stringValue(paramValue.apply("feishuSecret")));
        }
        if (hasParam.test("webhookIncomingToken")) {
            request.setWebhookIncomingToken(stringValue(paramValue.apply("webhookIncomingToken")));
        }
        if (hasParam.test("incomingToken")) {
            request.setIncomingToken(stringValue(paramValue.apply("incomingToken")));
        }
        if (hasParam.test("webhookTimeoutSeconds")) {
            request.setWebhookTimeoutSeconds(stringValue(paramValue.apply("webhookTimeoutSeconds")));
        }
        if (hasParam.test("timeoutSeconds")) {
            request.setTimeoutSeconds(stringValue(paramValue.apply("timeoutSeconds")));
        }
        if (hasParam.test("webhookLogRetentionDays")) {
            request.setWebhookLogRetentionDays(stringValue(paramValue.apply("webhookLogRetentionDays")));
        }
        if (hasParam.test("retentionDays")) {
            request.setRetentionDays(stringValue(paramValue.apply("retentionDays")));
        }
        if (hasParam.test("page")) {
            request.setPage(stringValue(paramValue.apply("page")));
        }
        if (hasParam.test("pageSize")) {
            request.setPageSize(stringValue(paramValue.apply("pageSize")));
        }
        if (hasParam.test("keyword")) {
            request.setKeyword(stringValue(paramValue.apply("keyword")));
        }
        if (hasParam.test("status")) {
            request.setStatus(stringValue(paramValue.apply("status")));
        }
        if (hasParam.test("direction")) {
            request.setDirection(stringValue(paramValue.apply("direction")));
        }
        if (hasParam.test("token")) {
            request.setToken(stringValue(paramValue.apply("token")));
        }
        if (hasParam.test("title")) {
            request.setTitle(stringValue(paramValue.apply("title")));
        }
        if (hasParam.test("content")) {
            request.setContent(stringValue(paramValue.apply("content")));
        }
        if (hasParam.test("text")) {
            request.setText(stringValue(paramValue.apply("text")));
        }
        if (hasParam.test("message")) {
            request.setMessage(stringValue(paramValue.apply("message")));
        }
        if (hasParam.test("source")) {
            request.setSource(stringValue(paramValue.apply("source")));
        }
        if (hasParam.test("requestId")) {
            request.setRequestId(stringValue(paramValue.apply("requestId")));
        }
        return request;
    }

    public boolean hasIncomingPayload() {
        return title != null || content != null || text != null || message != null || source != null || requestId != null;
    }

    private static String stringValue(Object value) {
        if (value instanceof String[]) {
            String[] values = (String[]) value;
            return values.length == 0 ? "" : values[0];
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return value == null ? "" : String.valueOf(value);
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getFeishuWebhookUrl() {
        return feishuWebhookUrl;
    }

    public void setFeishuWebhookUrl(String feishuWebhookUrl) {
        this.feishuWebhookUrl = feishuWebhookUrl;
    }

    public String getWebhookTargetType() {
        return webhookTargetType;
    }

    public void setWebhookTargetType(String webhookTargetType) {
        this.webhookTargetType = webhookTargetType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getWebhookSigningSecret() {
        return webhookSigningSecret;
    }

    public void setWebhookSigningSecret(String webhookSigningSecret) {
        this.webhookSigningSecret = webhookSigningSecret;
    }

    public String getFeishuSecret() {
        return feishuSecret;
    }

    public void setFeishuSecret(String feishuSecret) {
        this.feishuSecret = feishuSecret;
    }

    public String getWebhookIncomingToken() {
        return webhookIncomingToken;
    }

    public void setWebhookIncomingToken(String webhookIncomingToken) {
        this.webhookIncomingToken = webhookIncomingToken;
    }

    public String getIncomingToken() {
        return incomingToken;
    }

    public void setIncomingToken(String incomingToken) {
        this.incomingToken = incomingToken;
    }

    public String getWebhookTimeoutSeconds() {
        return webhookTimeoutSeconds;
    }

    public void setWebhookTimeoutSeconds(String webhookTimeoutSeconds) {
        this.webhookTimeoutSeconds = webhookTimeoutSeconds;
    }

    public String getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(String timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getWebhookLogRetentionDays() {
        return webhookLogRetentionDays;
    }

    public void setWebhookLogRetentionDays(String webhookLogRetentionDays) {
        this.webhookLogRetentionDays = webhookLogRetentionDays;
    }

    public String getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(String retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
