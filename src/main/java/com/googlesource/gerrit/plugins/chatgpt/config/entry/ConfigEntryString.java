package com.googlesource.gerrit.plugins.chatgpt.config.entry;

import com.google.gerrit.server.config.PluginConfig;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.config.entry.IConfigEntry;

import java.util.Map;

public class ConfigEntryString extends ConfigEntryBase implements IConfigEntry {

    public ConfigEntryString(String key) {
        super(key);
    }

    public void setCurrentConfigValue(Map<String, Object> configValues, PluginConfig projectConfig) {
        configValues.put(key, projectConfig.getString(key));
    }

    public void setDynamicConfigValue(Map<String, Object> configValues, String value) {
        configValues.put(key, value);
    }

    public void setMergedConfigValue(PluginConfig.Update configUpdater, Object value) {
        configUpdater.setString(key, (String)value);
    }
}
