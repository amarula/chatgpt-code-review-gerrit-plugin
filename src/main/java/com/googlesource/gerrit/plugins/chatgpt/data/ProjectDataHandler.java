package com.googlesource.gerrit.plugins.chatgpt.data;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectDataHandler {
    private static PluginDataHandler pluginDataHandler;

    public static void createNewInstance(PluginDataHandler loadedPluginDataHandler) {
        pluginDataHandler = loadedPluginDataHandler;
    }

    public static synchronized void setValue(GerritChange change, String key, String value) {
        pluginDataHandler.setValue(getProjectKey(change, key), value);
    }

    public static String getValue(GerritChange change, String key) {
        return pluginDataHandler.getValue(getProjectKey(change, key));
    }

    public static synchronized void removeValue(GerritChange change, String key) {
        pluginDataHandler.removeValue(getProjectKey(change, key));
    }

    private static String getProjectKey(GerritChange change, String key) {
        return change.getProjectName() + "." + key;
    }

}
