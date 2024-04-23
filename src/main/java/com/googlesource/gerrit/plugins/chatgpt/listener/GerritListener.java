package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class GerritListener implements EventListener {
    private final ConfigCreator configCreator;
    private final EventHandlerExecutor evenHandlerExecutor;

    @Inject
    public GerritListener(ConfigCreator configCreator, EventHandlerExecutor evenHandlerExecutor) {
        this.configCreator = configCreator;
        this.evenHandlerExecutor = evenHandlerExecutor;
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
            evenHandlerExecutor.execute(config, patchSetEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }

}
