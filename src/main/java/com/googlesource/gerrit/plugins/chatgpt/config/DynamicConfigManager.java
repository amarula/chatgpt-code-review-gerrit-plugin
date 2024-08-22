package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class DynamicConfigManager {
    public static final String KEY_DYNAMIC_CONFIG = "dynamicConfig";

    private final PluginDataHandler pluginDataHandler;
    @Getter
    private final Map<String, String> dynamicConfig;

    public DynamicConfigManager(PluginDataHandlerProvider pluginDataHandlerProvider) {
        this.pluginDataHandler = pluginDataHandlerProvider.getChangeScope();
        dynamicConfig = Optional.ofNullable(pluginDataHandler.getJsonObjectValue(KEY_DYNAMIC_CONFIG, String.class))
                .orElse(new HashMap<>());
        log.debug("Loaded dynamic configuration: {}", dynamicConfig);
    }

    public String getConfig(String key) {
        log.debug("Retrieving config key: {}", key);
        return dynamicConfig.get(key);
    }

    public void setConfig(String key, String value) {
        log.debug("Setting config key: {} with value: {}", key, value);
        dynamicConfig.put(key, value);
    }

    public void updateConfiguration(boolean shouldResetDynamicConfig, boolean modifiedDynamicConfig) {
        if (dynamicConfig == null || dynamicConfig.isEmpty()) {
            log.debug("Dynamic configuration is empty or null, skipping update.");
            return;
        }
        if (shouldResetDynamicConfig && !modifiedDynamicConfig) {
            log.debug("Resetting dynamic configuration without modification.");
            pluginDataHandler.removeValue(KEY_DYNAMIC_CONFIG);
        }
        else {
            if (shouldResetDynamicConfig) {
                log.debug("Resetting dynamic configuration.");
                resetDynamicConfig();
            }
            log.info("Updating dynamic configuration with {}", dynamicConfig);
            pluginDataHandler.setJsonValue(KEY_DYNAMIC_CONFIG, dynamicConfig);
        }
    }

    private void resetDynamicConfig() {
        // The keys with empty values are simply removed
        log.debug("Resetting dynamic configuration by removing empty or null values.");
        dynamicConfig.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        log.debug("Dynamic configuration after reset: {}", dynamicConfig);
    }
}
