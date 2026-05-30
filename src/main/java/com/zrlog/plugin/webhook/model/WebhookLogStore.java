package com.zrlog.plugin.webhook.model;

import java.util.ArrayList;
import java.util.List;

public class WebhookLogStore {

    private List<WebhookLogEntry> items = new ArrayList<>();

    public List<WebhookLogEntry> getItems() {
        return items;
    }

    public void setItems(List<WebhookLogEntry> items) {
        this.items = items;
    }
}
