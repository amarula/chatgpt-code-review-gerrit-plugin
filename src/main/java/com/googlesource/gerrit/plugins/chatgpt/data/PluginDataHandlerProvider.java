package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;

import java.nio.file.Path;

@Singleton
public class PluginDataHandlerProvider extends PluginDataHandlerBaseProvider implements Provider<PluginDataHandler> {
    private final String projectName;

    @Inject
    public PluginDataHandlerProvider(
            @com.google.gerrit.extensions.annotations.PluginData Path defaultPluginDataPath,
            GerritChange change
    ) {
        super(defaultPluginDataPath);
        projectName = change.getProjectName();
    }

    public PluginDataHandler getGlobalScope() {
        return super.get();
    }

    public PluginDataHandler getProjectScope() {
        return super.get(projectName);
    }
}
