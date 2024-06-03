package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class GerritListener implements EventListener {
    private final String myInstanceId;
    private final ConfigCreator configCreator;
    private final EventHandlerExecutor evenHandlerExecutor;

    @Inject
    public GerritListener(
            ConfigCreator configCreator,
            EventHandlerExecutor evenHandlerExecutor,
            @GerritInstanceId @Nullable String myInstanceId
    ) {
        this.configCreator = configCreator;
        this.evenHandlerExecutor = evenHandlerExecutor;
        this.myInstanceId = myInstanceId;
    }

    @Override
    public void onEvent(Event event) {
        if (!Objects.equals(event.instanceId, myInstanceId)) {
            log.debug("Ignore event from another instance");
            return;
        }
        if (!(event instanceof CommentAddedEvent || event instanceof PatchSetCreatedEvent)) {
            log.debug("The event is not a PatchSetCreatedEvent, it is: {}", event);
            return;
        }

        log.info("Processing event: {}", event);
        PatchSetEvent patchSetEvent = (PatchSetEvent) event;
        Project.NameKey projectNameKey = patchSetEvent.getProjectNameKey();
        Change.Key changeKey = patchSetEvent.getChangeKey();

        try {
            Configuration config = configCreator.createConfig(projectNameKey, changeKey);
            evenHandlerExecutor.execute(config, patchSetEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }
}
