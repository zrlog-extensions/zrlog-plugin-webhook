package com.zrlog.plugin.webhook.model;

public class WebhookSendRequest {

    private String sourcePluginId;
    private String sourcePluginName;
    private String sourceCapabilityKey;
    private String eventType;
    private String notificationType;
    private String channel;
    private String title;
    private String content;
    private String level;
    private String requestId;
    private String traceId;
    private Object payload;

    public String sourceText() {
        if (notBlank(sourcePluginName) || notBlank(notificationType)) {
            return "通知:" + (notBlank(sourcePluginName) ? sourcePluginName : notificationType);
        }
        return "服务调用";
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getSourcePluginId() {
        return sourcePluginId;
    }

    public void setSourcePluginId(String sourcePluginId) {
        this.sourcePluginId = sourcePluginId;
    }

    public String getSourcePluginName() {
        return sourcePluginName;
    }

    public void setSourcePluginName(String sourcePluginName) {
        this.sourcePluginName = sourcePluginName;
    }

    public String getSourceCapabilityKey() {
        return sourceCapabilityKey;
    }

    public void setSourceCapabilityKey(String sourceCapabilityKey) {
        this.sourceCapabilityKey = sourceCapabilityKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
