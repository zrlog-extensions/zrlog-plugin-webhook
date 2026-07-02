package com.zrlog.plugin.webhook.model;

import com.zrlog.plugin.message.Plugin;

public class WebhookPageData {

    private boolean dark;
    private String colorPrimary;
    private Plugin plugin;
    private WebhookConfig config;
    private Object summary;
    private Object trend;
    private Object logs;
    private String incomingPath;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public WebhookConfig getConfig() {
        return config;
    }

    public void setConfig(WebhookConfig config) {
        this.config = config;
    }

    public Object getSummary() {
        return summary;
    }

    public void setSummary(Object summary) {
        this.summary = summary;
    }

    public Object getTrend() {
        return trend;
    }

    public void setTrend(Object trend) {
        this.trend = trend;
    }

    public Object getLogs() {
        return logs;
    }

    public void setLogs(Object logs) {
        this.logs = logs;
    }

    public String getIncomingPath() {
        return incomingPath;
    }

    public void setIncomingPath(String incomingPath) {
        this.incomingPath = incomingPath;
    }
}
