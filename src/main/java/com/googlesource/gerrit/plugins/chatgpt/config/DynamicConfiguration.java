package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DynamicConfiguration {
    public static final String KEY_DYNAMIC_CONFIG = "dynamicConfig";

    private final PluginDataHandler pluginDataHandler;
    private final Map<String, String> dynamicConfig;

    public DynamicConfiguration(PluginDataHandlerProvider pluginDataHandlerProvider) {
        this.pluginDataHandler = pluginDataHandlerProvider.getChangeScope();
        dynamicConfig = pluginDataHandler.getJsonValue(KEY_DYNAMIC_CONFIG, String.class);
    }

    public void setConfig(String key, String value) {
        dynamicConfig.put(key, value);
    }

    public void updateConfiguration(boolean modifiedDynamicConfig, boolean shouldResetDynamicConfig) {
        if (dynamicConfig == null || dynamicConfig.isEmpty()) return;
        if (shouldResetDynamicConfig && !modifiedDynamicConfig) {
            pluginDataHandler.removeValue(DYNAMIC_CONFIG_KEY);
        }
        else {
            if (shouldResetDynamicConfig) {
                resetDynamicConfig();
            }
            log.info("Updating dynamic configuration with {}", dynamicConfig);
            pluginDataHandler.setJsonValue(KEY_DYNAMIC_CONFIG, dynamicConfig);
        }
    }

    private void resetDynamicConfig() {
        // The keys with empty values are simply removed
        dynamicConfig.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
    }
}
