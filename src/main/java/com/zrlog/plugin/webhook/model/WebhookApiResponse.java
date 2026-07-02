package com.zrlog.plugin.webhook.model;

public class WebhookApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public WebhookApiResponse() {
    }

    private WebhookApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static WebhookApiResponse<Void> success() {
        return new WebhookApiResponse<Void>(true, null, null);
    }

    public static <T> WebhookApiResponse<T> success(T data) {
        return new WebhookApiResponse<T>(true, null, data);
    }

    public static <T> WebhookApiResponse<T> error(String message, T data) {
        return new WebhookApiResponse<T>(false, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
