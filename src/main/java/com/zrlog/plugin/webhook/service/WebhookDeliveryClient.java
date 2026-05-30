package com.zrlog.plugin.webhook.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.client.HttpClientUtils;
import com.zrlog.plugin.common.type.HttpMethod;
import com.zrlog.plugin.data.codec.BaseHttpRequestInfo;
import com.zrlog.plugin.data.codec.HttpResponseInfo;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookSendResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebhookDeliveryClient {

    private final Gson gson = new Gson();

    public WebhookSendResult send(IOSession session, WebhookConfig config, Map<String, Object> payload) {
        WebhookSendResult result = new WebhookSendResult();
        try {
            BaseHttpRequestInfo requestInfo = new BaseHttpRequestInfo();
            requestInfo.setAccessUrl(config.getWebhookUrl());
            requestInfo.setHttpMethod(HttpMethod.POST);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");
            headers.put("Accept", "application/json");
            requestInfo.setHeader(headers);
            requestInfo.setRequestBody(gson.toJson(requestBody(config, payload)).getBytes(StandardCharsets.UTF_8));
            HttpResponseInfo responseInfo = HttpClientUtils.sendHttpRequest(requestInfo, session,
                    Duration.ofSeconds(Math.max(3, Math.min(60, config.getTimeoutSeconds()))));
            if (responseInfo == null) {
                result.setStatus(502);
                result.setError("HTTP response is null");
                return result;
            }
            result.setStatus(responseInfo.getStatusCode());
            String responseBody = responseInfo.getResponseBody() == null
                    ? "" : new String(responseInfo.getResponseBody(), StandardCharsets.UTF_8);
            result.setResponseBody(responseBody);
            if (responseInfo.getStatusCode() < 200 || responseInfo.getStatusCode() >= 300) {
                result.setError("HTTP " + responseInfo.getStatusCode() + ": " + responseBody);
                return result;
            }
            String remoteError = remoteError(config, responseBody);
            if (!remoteError.isEmpty()) {
                result.setError(remoteError);
                return result;
            }
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            result.setStatus(500);
            result.setError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return result;
        }
    }

    private Map<String, Object> requestBody(WebhookConfig config, Map<String, Object> payload) throws Exception {
        if (WebhookRepository.TARGET_FEISHU.equals(config.getTargetType())) {
            return feishuTextPayload(config, payload);
        }
        return genericJsonPayload(payload);
    }

    private Map<String, Object> genericJsonPayload(Map<String, Object> payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourcePluginId", payload.get("sourcePluginId"));
        body.put("sourcePluginName", payload.get("sourcePluginName"));
        body.put("sourceCapabilityKey", payload.get("sourceCapabilityKey"));
        body.put("eventType", payload.get("eventType"));
        body.put("notificationType", payload.get("notificationType"));
        body.put("channel", payload.get("channel"));
        body.put("title", payload.get("title"));
        body.put("content", payload.get("content"));
        body.put("level", payload.get("level"));
        body.put("requestId", payload.get("requestId"));
        body.put("traceId", payload.get("traceId"));
        body.put("payload", payload.get("payload"));
        return body;
    }

    private Map<String, Object> feishuTextPayload(WebhookConfig config, Map<String, Object> payload) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (notBlank(config.getSigningSecret())) {
            long timestamp = System.currentTimeMillis() / 1000;
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", sign(timestamp, config.getSigningSecret()));
        }
        body.put("msg_type", "text");
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("text", stringValue(payload.get("title")) + "\n" + stringValue(payload.get("content")));
        body.put("content", textContent);
        return body;
    }

    private String sign(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
    }

    private String remoteError(WebhookConfig config, String responseBody) {
        if (!WebhookRepository.TARGET_FEISHU.equals(config.getTargetType())) {
            return "";
        }
        if (!notBlank(responseBody) || !responseBody.trim().startsWith("{")) {
            return "";
        }
        try {
            Map response = gson.fromJson(responseBody, Map.class);
            Number code = numberValue(response.get("code"));
            if (code == null) {
                code = numberValue(response.get("statusCode"));
            }
            if (code == null) {
                code = numberValue(response.get("StatusCode"));
            }
            if (code != null && code.intValue() != 0) {
                Object message = response.get("msg");
                if (message == null) {
                    message = response.get("message");
                }
                return "Webhook " + code.intValue() + ": " + (message == null ? responseBody : String.valueOf(message));
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private Number numberValue(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
