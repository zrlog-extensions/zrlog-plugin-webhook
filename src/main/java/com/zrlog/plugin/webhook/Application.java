package com.zrlog.plugin.webhook;

import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.render.SimpleTemplateRender;
import com.zrlog.plugin.webhook.controller.WebhookController;
import com.zrlog.plugin.webhook.service.WebhookNotificationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static void main(String[] args) throws IOException {
        List<Class<?>> classList = new ArrayList<>();
        classList.add(WebhookController.class);
        new NioClient(null, new SimpleTemplateRender())
                .connectServer(args, classList, WebhookPluginAction.class, WebhookNotificationService.class);
    }
}
