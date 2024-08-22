package com.googlesource.gerrit.plugins.chatgpt.config;

import com.google.gerrit.server.config.PluginConfig;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.config.IConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.jsonArrayToList;

public class ConfigEntryList extends ConfigEntryBase implements IConfigEntry {

    public ConfigEntryList(String key) {
        super(key);
    }

    public void setCurrentConfigValue(Map<String, Object> configValues, PluginConfig projectConfig) {
        configValues.put(key, new ArrayList<>(Arrays.asList(projectConfig.getStringList(key))));
    }

    public void setDynamicConfigValue(Map<String, Object> configValues, String value) {
        configValues.put(key, jsonArrayToList(value));
    }

    @SuppressWarnings("unchecked")
    public void setMergedConfigValue(PluginConfig.Update configUpdater, Object value) {
        configUpdater.setStringList(key, (List<String>) value);
    }
}
