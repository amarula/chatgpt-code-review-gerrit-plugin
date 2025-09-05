/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.PatchSetReviewer;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.listener.IEventHandlerType;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EventHandlerTask implements Runnable {
    @VisibleForTesting
    public enum Result {
        OK, NOT_SUPPORTED, FAILURE
    }
    public enum SupportedEvents {
        PATCH_SET_CREATED,
        COMMENT_ADDED,
        CHANGE_MERGED
    }

    public static final Map<SupportedEvents, Class<?>> EVENT_CLASS_MAP = Map.of(
            SupportedEvents.PATCH_SET_CREATED, PatchSetCreatedEvent.class,
            SupportedEvents.COMMENT_ADDED, CommentAddedEvent.class,
            SupportedEvents.CHANGE_MERGED, ChangeMergedEvent.class
    );

    private static final Map<String, SupportedEvents> EVENT_TYPE_MAP = Map.of(
            "patchset-created", SupportedEvents.PATCH_SET_CREATED,
            "comment-added", SupportedEvents.COMMENT_ADDED,
            "change-merged", SupportedEvents.CHANGE_MERGED
    );

    private final Configuration config;
    private final GerritClient gerritClient;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final PatchSetReviewer reviewer;
    private final ICodeContextPolicy codeContextPolicy;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;

    private SupportedEvents processing_event_type;
    private IEventHandlerType eventHandlerType;

    @Inject
    EventHandlerTask(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            PatchSetReviewer reviewer,
            GerritClient gerritClient,
            ICodeContextPolicy codeContextPolicy,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        this.changeSetData = changeSetData;
        this.change = change;
        this.reviewer = reviewer;
        this.gerritClient = gerritClient;
        this.config = config;
        this.codeContextPolicy = codeContextPolicy;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        log.debug("EventHandlerTask initialized for change ID: {}", change.getFullChangeId());
    }

    @Override
    public void run() {
        log.debug("EventHandlerTask started for event type: {}", change.getEventType());
        Result result = execute();
        log.debug("EventHandlerTask execution completed with result: {}", result);
    }

    @VisibleForTesting
    public Result execute() {
        log.debug("Starting event processing for change ID: {}", change.getFullChangeId());
        if (!preProcessEvent()) {
            log.debug("Preprocessing event not supported or failed for event type: {}", change.getEventType());
            return Result.NOT_SUPPORTED;
        }

        try {
            log.info("Processing event for change ID:: {}", change.getFullChangeId());
            eventHandlerType.processEvent();
            log.info("Finished processing event for change ID: {}", change.getFullChangeId());
        } catch (Exception e) {
            log.error("Error while processing event for change ID: {}", change.getFullChangeId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Result.FAILURE;
        }
        return Result.OK;
    }

    private boolean preProcessEvent() {
        String eventType = Optional.ofNullable(change.getEventType()).orElse("");
        processing_event_type = EVENT_TYPE_MAP.get(eventType);
        if (processing_event_type == null) {
            log.debug("Event type not supported: {}", eventType);
            return false;
        }

        if (!isReviewEnabled(change)) {
            log.debug("Review not enabled for event type: {}", eventType);
            return false;
        }

        while (true) {
            eventHandlerType = getEventHandlerType();
            log.debug("Event handler type resolved for event: {}", eventType);
            switch (eventHandlerType.preprocessEvent()) {
                case EXIT -> {
                    log.debug("Exiting event handler preprocessing for event type: {}", eventType);
                    return false;
                }
                case SWITCH_TO_PATCH_SET_CREATED -> {
                    log.debug("Switching to patch set created event type");
                    processing_event_type = SupportedEvents.PATCH_SET_CREATED;
                    continue;
                }
            }
            break;
        }
        log.debug("Preprocessing completed successfully for event type: {}", eventType);
        return true;
    }

    private IEventHandlerType getEventHandlerType() {
        return switch (processing_event_type) {
            case PATCH_SET_CREATED -> new EventHandlerTypePatchSetReview(config, changeSetData, change, reviewer, gerritClient);
            case COMMENT_ADDED -> new EventHandlerTypeCommentAdded(changeSetData, change, reviewer, gerritClient);
            case CHANGE_MERGED -> new EventHandlerTypeChangeMerged(config, changeSetData, change, codeContextPolicy, pluginDataHandlerProvider);
        };
    }

    private boolean isReviewEnabled(GerritChange change) {
        List<String> enabledProjects = Splitter.on(",").omitEmptyStrings()
                .splitToList(config.getEnabledProjects());
        if (!config.isGlobalEnable() &&
                !enabledProjects.contains(change.getProjectNameKey().get()) &&
                !config.isProjectEnable()) {
            log.debug("The project {} is not enabled for review", change.getProjectNameKey());
            return false;
        }

        String topic = getTopic(change).orElse("");
        log.debug("PatchSet Topic retrieved: '{}'", topic);
        if (gerritClient.isDisabledTopic(topic)) {
            log.info("Review disabled for topic: '{}'", topic);
            return false;
        }
        return true;
    }

    private Optional<String> getTopic(GerritChange change) {
        try {
            ChangeAttribute changeAttribute = change.getPatchSetEvent().change.get();
            return Optional.ofNullable(changeAttribute.topic);
        } catch (NullPointerException e) {
            log.debug("Failed to retrieve topic for change ID: {}", change.getFullChangeId());
            return Optional.empty();
        }
    }
}
