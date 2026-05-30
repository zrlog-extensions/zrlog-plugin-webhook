package com.zrlog.plugin.webhook.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.type.ActionType;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookLogEntry;
import com.zrlog.plugin.webhook.model.WebhookLogStore;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebhookRepository {

    public static final String DIRECTION_OUTBOUND = "outbound";
    public static final String DIRECTION_INBOUND = "inbound";
    public static final String CHANNEL_WEBHOOK = "webhook";
    public static final String CHANNEL_INCOMING = "incoming";
    public static final String TARGET_FEISHU = "feishu";
    public static final String TARGET_GENERIC_JSON = "generic_json";

    private static final Logger LOGGER = LoggerUtil.getLogger(WebhookRepository.class);
    private static final WebhookRepository INSTANCE = new WebhookRepository();
    private static final String STORE_KEY = "webhookLogs";
    private static final String WEBHOOK_URL_KEY = "webhookUrl";
    private static final String TARGET_TYPE_KEY = "webhookTargetType";
    private static final String SIGNING_SECRET_KEY = "webhookSigningSecret";
    private static final String INCOMING_TOKEN_KEY = "webhookIncomingToken";
    private static final String TIMEOUT_SECONDS_KEY = "webhookTimeoutSeconds";
    private static final String RETENTION_DAYS_KEY = "webhookLogRetentionDays";
    private static final String LEGACY_FEISHU_WEBHOOK_URL_KEY = "feishuWebhookUrl";
    private static final String LEGACY_FEISHU_SECRET_KEY = "feishuSecret";
    private static final String CONFIG_KEYS = WEBHOOK_URL_KEY + "," + TARGET_TYPE_KEY + "," + SIGNING_SECRET_KEY
            + "," + LEGACY_FEISHU_WEBHOOK_URL_KEY + "," + LEGACY_FEISHU_SECRET_KEY
            + "," + INCOMING_TOKEN_KEY + "," + TIMEOUT_SECONDS_KEY + "," + RETENTION_DAYS_KEY;
    private static final int DEFAULT_RETENTION_DAYS = 30;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int MAX_VALUE_BYTES = 950 * 1024;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final Gson gson = new Gson();

    public static WebhookRepository getInstance() {
        return INSTANCE;
    }

    public synchronized WebhookConfig readConfig(IOSession session) {
        Map<String, String> request = new HashMap<>();
        request.put("key", CONFIG_KEYS);
        Map responseMap = session.getResponseSync(ContentType.JSON, request, ActionType.GET_WEBSITE, Map.class);
        WebhookConfig config = new WebhookConfig();
        if (responseMap != null) {
            config.setWebhookUrl(firstNonBlank(stringValue(responseMap.get(WEBHOOK_URL_KEY)),
                    stringValue(responseMap.get(LEGACY_FEISHU_WEBHOOK_URL_KEY))));
            config.setTargetType(normalizeTargetType(stringValue(responseMap.get(TARGET_TYPE_KEY))));
            config.setSigningSecret(firstNonBlank(stringValue(responseMap.get(SIGNING_SECRET_KEY)),
                    stringValue(responseMap.get(LEGACY_FEISHU_SECRET_KEY))));
            config.setIncomingToken(stringValue(responseMap.get(INCOMING_TOKEN_KEY)));
            config.setTimeoutSeconds(normalizeTimeoutSeconds(stringValue(responseMap.get(TIMEOUT_SECONDS_KEY))));
            config.setRetentionDays(normalizeRetentionDays(stringValue(responseMap.get(RETENTION_DAYS_KEY))));
        }
        if (!notBlank(config.getIncomingToken())) {
            config.setIncomingToken(newToken());
            writeWebsiteValue(session, INCOMING_TOKEN_KEY, config.getIncomingToken());
        }
        return config;
    }

    public synchronized WebhookConfig saveConfig(IOSession session, Map<String, Object> params) {
        WebhookConfig existing = readConfig(session);
        WebhookConfig config = new WebhookConfig();
        config.setWebhookUrl(limit(valueOrExisting(params, existing.getWebhookUrl(),
                WEBHOOK_URL_KEY, LEGACY_FEISHU_WEBHOOK_URL_KEY), 1000));
        config.setTargetType(normalizeTargetType(valueOrExisting(params, existing.getTargetType(),
                TARGET_TYPE_KEY, "targetType")));
        config.setSigningSecret(limit(valueOrExisting(params, existing.getSigningSecret(),
                SIGNING_SECRET_KEY, LEGACY_FEISHU_SECRET_KEY), 240));
        String incomingToken = limit(stringValue(params.get("incomingToken")), 160);
        config.setIncomingToken(notBlank(incomingToken) ? incomingToken : existing.getIncomingToken());
        String timeoutSeconds = stringValue(params.get(TIMEOUT_SECONDS_KEY));
        if (!notBlank(timeoutSeconds)) {
            timeoutSeconds = stringValue(params.get("timeoutSeconds"));
        }
        config.setTimeoutSeconds(normalizeTimeoutSeconds(timeoutSeconds));
        String retentionDays = stringValue(params.get(RETENTION_DAYS_KEY));
        if (!notBlank(retentionDays)) {
            retentionDays = stringValue(params.get("retentionDays"));
        }
        config.setRetentionDays(normalizeRetentionDays(retentionDays));

        Map<String, String> request = new HashMap<>();
        request.put(WEBHOOK_URL_KEY, config.getWebhookUrl());
        request.put(TARGET_TYPE_KEY, config.getTargetType());
        request.put(SIGNING_SECRET_KEY, config.getSigningSecret());
        request.put(INCOMING_TOKEN_KEY, config.getIncomingToken());
        request.put(TIMEOUT_SECONDS_KEY, String.valueOf(config.getTimeoutSeconds()));
        request.put(RETENTION_DAYS_KEY, String.valueOf(config.getRetentionDays()));
        session.getResponseSync(ContentType.JSON, request, ActionType.SET_WEBSITE, Map.class);
        return config;
    }

    public synchronized void record(IOSession session, String direction, String channel, String title, String content,
                                    String source, boolean success, int status, String error, String requestId) {
        try {
            WebhookConfig config = readConfig(session);
            WebhookLogStore store = readStore(session);
            WebhookLogEntry entry = new WebhookLogEntry();
            entry.setTimestamp(System.currentTimeMillis());
            entry.setDirection(limit(direction, 24));
            entry.setChannel(limit(channel, 40));
            entry.setTitle(limit(title, 180));
            entry.setContent(limit(content, 800));
            entry.setSource(limit(source, 80));
            entry.setSuccess(success);
            entry.setStatus(status);
            entry.setError(limit(error, 500));
            entry.setRequestId(limit(requestId, 160));
            store.getItems().add(entry);
            writeStore(session, prune(store, config.getRetentionDays()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "record webhook log error", e);
        }
    }

    public synchronized Map<String, Object> overview(IOSession session, int retentionDays) {
        List<WebhookLogEntry> logs = listRecentLogs(session, retentionDays);
        int outbound = 0;
        int inbound = 0;
        int failed = 0;
        Map<String, Integer> dayCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZONE);
        for (int i = retentionDays - 1; i >= 0; i--) {
            dayCounts.put(today.minusDays(i).toString(), 0);
        }
        for (WebhookLogEntry log : logs) {
            if (DIRECTION_OUTBOUND.equals(log.getDirection())) {
                outbound++;
            }
            if (DIRECTION_INBOUND.equals(log.getDirection())) {
                inbound++;
            }
            if (!log.isSuccess()) {
                failed++;
            }
            LocalDate day = toDay(log.getTimestamp());
            if (day != null) {
                String key = day.toString();
                dayCounts.put(key, dayCounts.getOrDefault(key, 0) + 1);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", metrics(retentionDays, logs.size(), outbound, inbound, failed));
        data.put("trend", trend(dayCounts));
        return data;
    }

    public synchronized Map<String, Object> page(IOSession session, Map<String, Object> params, int retentionDays) {
        int page = Math.max(1, parseInt(stringValue(params.get("page")), 1));
        int pageSize = Math.max(1, Math.min(100, parseInt(stringValue(params.get("pageSize")), 10)));
        String keyword = stringValue(params.get("keyword")).toLowerCase();
        String status = stringValue(params.get("status"));
        String direction = stringValue(params.get("direction"));
        List<WebhookLogEntry> logs = listRecentLogs(session, retentionDays);
        Collections.sort(logs, new Comparator<WebhookLogEntry>() {
            @Override
            public int compare(WebhookLogEntry left, WebhookLogEntry right) {
                return Long.compare(right.getTimestamp(), left.getTimestamp());
            }
        });
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (WebhookLogEntry log : logs) {
            if ("success".equals(status) && !log.isSuccess()) {
                continue;
            }
            if ("failed".equals(status) && log.isSuccess()) {
                continue;
            }
            if (notBlank(direction) && !direction.equals(log.getDirection())) {
                continue;
            }
            Map<String, Object> row = rowMap(log);
            if (notBlank(keyword) && !gson.toJson(row).toLowerCase().contains(keyword)) {
                continue;
            }
            filtered.add(row);
        }
        int total = filtered.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", filtered.subList(from, to));
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    private WebhookLogStore readStore(IOSession session) {
        try {
            String json = readWebsiteValue(session, STORE_KEY);
            if (!notBlank(json)) {
                return new WebhookLogStore();
            }
            WebhookLogStore store = gson.fromJson(json, WebhookLogStore.class);
            return store == null ? new WebhookLogStore() : store;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read webhook log store error", e);
            return new WebhookLogStore();
        }
    }

    private void writeStore(IOSession session, WebhookLogStore store) {
        String json = gson.toJson(store);
        while (json.getBytes(StandardCharsets.UTF_8).length > MAX_VALUE_BYTES && !store.getItems().isEmpty()) {
            store.getItems().remove(0);
            json = gson.toJson(store);
        }
        writeWebsiteValue(session, STORE_KEY, json);
    }

    private WebhookLogStore prune(WebhookLogStore store, int retentionDays) {
        long minTime = LocalDate.now(ZONE).minusDays(retentionDays - 1L).atStartOfDay(ZONE).toInstant().toEpochMilli();
        List<WebhookLogEntry> items = new ArrayList<>();
        for (WebhookLogEntry item : store.getItems()) {
            if (item.getTimestamp() >= minTime) {
                items.add(item);
            }
        }
        store.setItems(items);
        return store;
    }

    private List<WebhookLogEntry> listRecentLogs(IOSession session, int retentionDays) {
        return prune(readStore(session), retentionDays).getItems();
    }

    private List<Map<String, Object>> metrics(int retentionDays, int total, int outbound, int inbound, int failed) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metricMap(retentionDays + " 天记录", total, total > 0 ? "processing" : "normal"));
        metrics.add(metricMap("通知推送", outbound, outbound > 0 ? "processing" : "normal"));
        metrics.add(metricMap("入站请求", inbound, inbound > 0 ? "processing" : "normal"));
        metrics.add(metricMap("失败记录", failed, failed > 0 ? "warning" : "normal"));
        return metrics;
    }

    private List<Map<String, Object>> trend(Map<String, Integer> dayCounts) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dayCounts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", entry.getKey().substring(5));
            row.put("value", entry.getValue());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> rowMap(WebhookLogEntry log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(log.getTimestamp()) + "-" + log.getDirection() + "-" + log.getStatus());
        row.put("timestamp", log.getTimestamp());
        row.put("time", formatTime(log.getTimestamp()));
        row.put("direction", defaultText(log.getDirection(), ""));
        row.put("channel", defaultText(log.getChannel(), ""));
        row.put("title", defaultText(log.getTitle(), ""));
        row.put("content", defaultText(log.getContent(), ""));
        row.put("source", defaultText(log.getSource(), ""));
        row.put("success", log.isSuccess());
        row.put("status", log.getStatus());
        row.put("error", defaultText(log.getError(), ""));
        row.put("requestId", defaultText(log.getRequestId(), ""));
        return row;
    }

    private Map<String, Object> metricMap(String label, int value, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", label);
        map.put("value", value);
        map.put("status", status);
        return map;
    }

    private String readWebsiteValue(IOSession session, String key) {
        Map<String, String> request = new HashMap<>();
        request.put("key", key);
        Map responseMap = session.getResponseSync(ContentType.JSON, request, ActionType.GET_WEBSITE, Map.class);
        if (responseMap == null || responseMap.get(key) == null) {
            return "";
        }
        return String.valueOf(responseMap.get(key));
    }

    private void writeWebsiteValue(IOSession session, String key, String value) {
        Map<String, String> request = new HashMap<>();
        request.put(key, value);
        session.getResponseSync(ContentType.JSON, request, ActionType.SET_WEBSITE, Map.class);
    }

    private int normalizeRetentionDays(String value) {
        int days = parseInt(value, DEFAULT_RETENTION_DAYS);
        if (days == 90 || days == 180) {
            return days;
        }
        return DEFAULT_RETENTION_DAYS;
    }

    private int normalizeTimeoutSeconds(String value) {
        int seconds = parseInt(value, DEFAULT_TIMEOUT_SECONDS);
        return Math.max(3, Math.min(60, seconds));
    }

    private String normalizeTargetType(String value) {
        if (TARGET_GENERIC_JSON.equals(value)) {
            return TARGET_GENERIC_JSON;
        }
        return TARGET_FEISHU;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            if (!notBlank(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return String.valueOf(value);
    }

    private String valueOrExisting(Map<String, Object> params, String existingValue, String... keys) {
        if (params != null && keys != null) {
            for (String key : keys) {
                if (params.containsKey(key)) {
                    return stringValue(params.get(key));
                }
            }
        }
        return existingValue == null ? "" : existingValue;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private LocalDate toDay(long time) {
        if (time <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZONE).toLocalDate();
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    private String defaultText(String value, String defaultValue) {
        return notBlank(value) ? value : defaultValue;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
