package com.zrlog.plugin.webhook.model;

public class WebhookRemoteResponse {

    private Number code;
    private Number statusCode;
    private Number StatusCode;
    private String msg;
    private String message;

    public Number errorCode() {
        if (code != null) {
            return code;
        }
        if (statusCode != null) {
            return statusCode;
        }
        return StatusCode;
    }

    public String errorMessage() {
        return msg == null ? message : msg;
    }

    public Number getCode() {
        return code;
    }

    public void setCode(Number code) {
        this.code = code;
    }

    public Number getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Number statusCode) {
        this.statusCode = statusCode;
    }

    public Number getStatusCodeUpper() {
        return StatusCode;
    }

    public void setStatusCodeUpper(Number statusCodeUpper) {
        this.StatusCode = statusCodeUpper;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
