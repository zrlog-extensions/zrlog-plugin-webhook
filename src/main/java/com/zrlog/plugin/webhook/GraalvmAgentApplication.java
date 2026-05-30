package com.zrlog.plugin.webhook;

import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.webhook.controller.WebhookController;
import com.zrlog.plugin.webhook.model.WebhookConfig;
import com.zrlog.plugin.webhook.model.WebhookLogEntry;
import com.zrlog.plugin.webhook.model.WebhookLogStore;
import com.zrlog.plugin.webhook.model.WebhookSendResult;
import com.zrlog.plugin.webhook.service.WebhookNotificationService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class GraalvmAgentApplication {

    public static void main(String[] args) throws IOException {
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(
                WebhookConfig.class,
                WebhookLogEntry.class,
                WebhookLogStore.class,
                WebhookSendResult.class
        ));
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        PluginNativeImageUtils.exposeController(Collections.singletonList(WebhookController.class));
        PluginNativeImageUtils.usedGsonObject();
        exposePluginReflectivePaths();
        Application.main(args);
    }

    private static void exposePluginReflectivePaths() {
        try {
            WebhookPluginAction.class.newInstance();
            WebhookNotificationService.class.newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
