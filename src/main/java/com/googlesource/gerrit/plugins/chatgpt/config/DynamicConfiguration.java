package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class DynamicConfiguration {
    public static final String KEY_DYNAMIC_CONFIG = "dynamicConfig";

    private final PluginDataHandler pluginDataHandler;
    private final Map<String, String> dynamicConfig;

    public DynamicConfiguration(PluginDataHandlerProvider pluginDataHandlerProvider) {
        this.pluginDataHandler = pluginDataHandlerProvider.getChangeScope();
        dynamicConfig = Optional.ofNullable(pluginDataHandler.getJsonValue(KEY_DYNAMIC_CONFIG, String.class))
                .orElse(new HashMap<>());
    }

    public void setConfig(String key, String value) {
        dynamicConfig.put(key, value);
    }

    public boolean hasConfig() {
        return !dynamicConfig.isEmpty();
    }

    public void updateConfiguration(boolean modifiedDynamicConfig, boolean shouldResetDynamicConfig) {
        if (dynamicConfig == null || dynamicConfig.isEmpty()) return;
        if (shouldResetDynamicConfig && !modifiedDynamicConfig) {
            pluginDataHandler.removeValue(KEY_DYNAMIC_CONFIG);
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
