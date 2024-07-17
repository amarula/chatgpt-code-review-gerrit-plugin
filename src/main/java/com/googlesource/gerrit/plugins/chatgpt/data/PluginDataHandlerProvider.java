package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.sanitizeFilename;

@Singleton
@Slf4j
public class PluginDataHandlerProvider extends PluginDataHandlerBaseProvider implements Provider<PluginDataHandler> {
    private static final String PATH_ASSISTANTS = ".assistants";

    private final String projectName;
    private final String changeKey;
    private final String assistantsWorkspace;

    @Inject
    public PluginDataHandlerProvider(
            @com.google.gerrit.extensions.annotations.PluginData Path defaultPluginDataPath,
            GerritChange change
    ) {
        super(defaultPluginDataPath);
        projectName = sanitizeFilename(change.getProjectName());
        changeKey = change.getChangeKey().toString();
        assistantsWorkspace = projectName + PATH_ASSISTANTS;
        log.debug("PluginDataHandlerProvider initialized for project: {}, change key: {}, workspace: {}",
                projectName, changeKey, assistantsWorkspace);
    }

    public PluginDataHandler getGlobalScope() {
        log.debug("Accessing global scope PluginDataHandler");
        return super.get();
    }

    public PluginDataHandler getProjectScope() {
        log.debug("Accessing PluginDataHandler for project scope: {}", projectName);
        return super.get(projectName);
    }

    public PluginDataHandler getChangeScope() {
        log.debug("Accessing PluginDataHandler for change scope: {}", changeKey);
        return super.get(changeKey);
    }

    public PluginDataHandler getAssistantsWorkspace() {
        log.debug("Accessing PluginDataHandler for assistants workspace: {}", assistantsWorkspace);
        return super.get(assistantsWorkspace);
    }
}
