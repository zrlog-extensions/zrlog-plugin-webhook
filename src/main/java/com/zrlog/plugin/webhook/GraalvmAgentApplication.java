package com.zrlog.plugin.webhook;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.type.RunType;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraalvmAgentApplication {

    private static final Logger LOGGER = LoggerUtil.getLogger(GraalvmAgentApplication.class);

    public static void main(String[] args) throws IOException {
        RunConstants.runType = RunType.AGENT;
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
            WebhookPluginAction.class.getDeclaredConstructor().newInstance();
            WebhookNotificationService.class.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Expose webhook reflective paths failed", e);
        }
    }
}
