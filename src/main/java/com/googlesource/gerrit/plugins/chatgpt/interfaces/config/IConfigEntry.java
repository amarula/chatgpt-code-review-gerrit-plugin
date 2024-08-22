package com.googlesource.gerrit.plugins.chatgpt.interfaces.config;

import com.google.gerrit.server.config.PluginConfig;

import java.util.Map;

public interface IConfigEntry {
    void setCurrentConfigValue(Map<String, Object> configValues, PluginConfig projectConfig);
    void setDynamicConfigValue(Map<String, Object> configValues, String value);
    void setMergedConfigValue(PluginConfig.Update configUpdater, Object value);
}
