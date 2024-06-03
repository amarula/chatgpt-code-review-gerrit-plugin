package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.gerrit.extensions.client.ChangeKind;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.PatchSetReviewer;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.gerrit.extensions.client.ChangeKind.REWORK;

@Slf4j
public class EventHandlerTask implements Runnable {
    @VisibleForTesting
    public enum Result {
        OK, NOT_SUPPORTED, FAILURE
    }

    private final static Map<String, Boolean> EVENT_COMMENT_MAP = Map.of(
            "patchset-created", false,
            "comment-added", true
    );

    private final Configuration config;
    private final GerritClient gerritClient;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final PatchSetReviewer reviewer;

    @Inject
    EventHandlerTask(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            PatchSetReviewer reviewer,
            GerritClient gerritClient
    ) {
        this.changeSetData = changeSetData;
        this.change = change;
        this.reviewer = reviewer;
        this.gerritClient = gerritClient;
        this.config = config;
    }

    @Override
    public void run() {
        execute();
    }

    @VisibleForTesting
    public Result execute() {
        if (!preProcessEvent(change, changeSetData)) {
            return Result.NOT_SUPPORTED;
        }

        try {
            log.info("Processing change: {}", change.getFullChangeId());
            reviewer.review(change);
            log.info("Finished processing change: {}", change.getFullChangeId());
        } catch (Exception e) {
            log.error("Error while processing change: {}", change.getFullChangeId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Result.FAILURE;
        }
        return Result.OK;
    }

    private boolean preProcessEvent(GerritChange change, ChangeSetData changeSetData) {
        String eventType = Optional.ofNullable(change.getEventType()).orElse("");
        log.info("Event type {}", eventType);
        if (!EVENT_COMMENT_MAP.containsKey(eventType)) {
            return false;
        }

        if (!isReviewEnabled(change)) {
            return false;
        }
        boolean isCommentEvent = EVENT_COMMENT_MAP.get(eventType);
        if (isCommentEvent) {
            if (!gerritClient.retrieveLastComments(change)) {
                if (changeSetData.getForcedReview() || changeSetData.getForceDisplaySystemMessage()) {
                    isCommentEvent = false;
                } else {
                    log.info("No comments found for review");
                    return false;
                }
            }
        } else {
            if (!isPatchSetReviewEnabled(change)) {
                log.debug("Patch Set review disabled");
                return false;
            }
        }
        log.debug("Flag `isCommentEvent` set to {}", isCommentEvent);
        change.setIsCommentEvent(isCommentEvent);
        if (!isCommentEvent) {
            gerritClient.retrievePatchSetInfo(change);
        }

        return true;
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
            log.info("Disabled review for PatchSets with Topic '{}'", topic);
            return false;
        }

        return true;
    }

    private boolean isPatchSetReviewEnabled(GerritChange change) {
        if (!config.getGptReviewPatchSet()) {
            log.debug("Disabled review function for created or updated PatchSets.");
            return false;
        }
        Optional<PatchSetAttribute> patchSetAttributeOptional = change.getPatchSetAttribute();
        if (patchSetAttributeOptional.isEmpty()) {
            log.info("PatchSetAttribute event properties not retrieved");
            return false;
        }
        PatchSetAttribute patchSetAttribute = patchSetAttributeOptional.get();
        ChangeKind patchSetEventKind = patchSetAttribute.kind;
        if (patchSetEventKind != REWORK) {
            log.debug("Change kind '{}' not processed", patchSetEventKind);
            return false;
        }
        String authorUsername = patchSetAttribute.author.username;
        if (gerritClient.isDisabledUser(authorUsername)) {
            log.info("Review of PatchSets from user '{}' is disabled.", authorUsername);
            return false;
        }
        if (gerritClient.isWorkInProgress(change)) {
            log.debug("Skipping Patch Set processing due to its WIP status.");
            return false;
        }
        return true;
    }

    private Optional<String> getTopic(GerritChange change) {
        try {
            ChangeAttribute changeAttribute = change.getPatchSetEvent().change.get();
            return Optional.ofNullable(changeAttribute.topic);
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }
}
