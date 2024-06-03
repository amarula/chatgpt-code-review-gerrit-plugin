package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DynamicConfiguration {
    public static final String DYNAMIC_CONFIG_KEY = "dynamicConfig";

    private final PluginDataHandler pluginDataHandler;
    private final Map<String, String> dynamicConfig;

    public DynamicConfiguration(PluginDataHandlerProvider pluginDataHandlerProvider) {
        this.pluginDataHandler = pluginDataHandlerProvider.getChangeScope();
        dynamicConfig = pluginDataHandler.getJsonValue(DYNAMIC_CONFIG_KEY, String.class);
    }

    public void setConfig(String key, String value) {
        dynamicConfig.put(key, value);
    }

    public void updateConfiguration() {
        if (dynamicConfig != null && !dynamicConfig.isEmpty()) {
            log.info("Updating dynamic configuration with {}", dynamicConfig);
            pluginDataHandler.setJsonValue(DYNAMIC_CONFIG_KEY, dynamicConfig);
        }
    }
}
