package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerritListener implements EventListener {
    private final ConfigCreator configCreator;
    private final EventListenerHandler eventListenerHandler;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler pluginDataHandler;

    @Inject
    public GerritListener(
            ConfigCreator configCreator,
            EventListenerHandler eventListenerHandler,
            GitRepoFiles gitRepoFiles,
            PluginDataHandler pluginDataHandler
    ) {
        this.configCreator = configCreator;
        this.eventListenerHandler = eventListenerHandler;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandler = pluginDataHandler;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof CommentAddedEvent || event instanceof PatchSetCreatedEvent)) {
            log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);
        PatchSetEvent patchSetEvent = (PatchSetEvent) event;
        Project.NameKey projectNameKey = patchSetEvent.getProjectNameKey();

        try {
            Configuration config = configCreator.createConfig(projectNameKey);
            eventListenerHandler.handleEvent(config, patchSetEvent, gitRepoFiles, pluginDataHandler);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }

}
