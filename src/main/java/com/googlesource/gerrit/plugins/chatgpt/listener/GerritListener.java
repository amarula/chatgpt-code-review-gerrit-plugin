package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.*;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerBaseProvider;
import com.googlesource.gerrit.plugins.chatgpt.logging.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.EVENT_CLASS_MAP;

@Slf4j
public class GerritListener implements EventListener {
    private final String myInstanceId;
    private final ConfigCreator configCreator;
    private final EventHandlerExecutor evenHandlerExecutor;
    private final PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider;

    @Inject
    public GerritListener(
            ConfigCreator configCreator,
            EventHandlerExecutor evenHandlerExecutor,
            PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider,
            @GerritInstanceId @Nullable String myInstanceId
    ) {
        this.configCreator = configCreator;
        this.evenHandlerExecutor = evenHandlerExecutor;
        this.pluginDataHandlerBaseProvider = pluginDataHandlerBaseProvider;
        this.myInstanceId = myInstanceId;
    }

    @Override
    public void onEvent(Event event) {
        if (!Objects.equals(event.instanceId, myInstanceId)) {
            log.debug("Ignore event from another instance");
            return;
        }
        if (!EVENT_CLASS_MAP.containsValue(event.getClass())) {
            log.debug("The event {} is not managed by the plugin", event);
            return;
        }

        log.info("Processing event: {}", event);
        PatchSetEvent patchSetEvent = (PatchSetEvent) event;
        Project.NameKey projectNameKey = patchSetEvent.getProjectNameKey();
        Change.Key changeKey = patchSetEvent.getChangeKey();

        try {
            Configuration config = configCreator.createConfig(projectNameKey, changeKey);
            LoggingConfiguration.configure(config, pluginDataHandlerBaseProvider);
            evenHandlerExecutor.execute(config, patchSetEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }
}
