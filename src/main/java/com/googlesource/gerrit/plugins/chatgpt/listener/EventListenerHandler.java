package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gerrit.extensions.client.ChangeKind;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.PatchSetReviewer;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.settings.Settings;
import com.googlesource.gerrit.plugins.chatgpt.settings.DynamicSettings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static com.google.gerrit.extensions.client.ChangeKind.REWORK;

@Slf4j
public class EventListenerHandler {
    private final static Map<String, Boolean> EVENT_COMMENT_MAP = Map.of(
            "patchset-created", false,
            "comment-added", true
    );

    private final PatchSetReviewer reviewer;
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
    private final RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("EventListenerHandler-%d")
            .build();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, queue, threadFactory, handler);
    private final GerritClient gerritClient;
    private Configuration config;
    private Settings settings;
    @Getter
    private CompletableFuture<Void> latestFuture;

    @Inject
    public EventListenerHandler(PatchSetReviewer reviewer, GerritClient gerritClient) {
        this.reviewer = reviewer;
        this.gerritClient = gerritClient;

        addShutdownHoot();
    }

    public void initialize(GerritChange change) {
        gerritClient.initialize(config, change);
        Integer gptAccountId = gerritClient.getNotNullAccountId(change, config.getGerritUserName());
        settings = DynamicSettings.getNewInstance(config, change, gptAccountId);
    }

    public void handleEvent(Configuration config, Event event) {
        this.config = config;
        GerritChange change = new GerritChange(event);
        initialize(change);

        if (!preProcessEvent(change)) {
            destroy(change);
            return;
        }

        // Execute the potentially time-consuming operation asynchronously
        latestFuture = CompletableFuture.runAsync(() -> {
            try {
                log.info("Processing change: {}", change.getFullChangeId());
                reviewer.review(config, change);
                log.info("Finished processing change: {}", change.getFullChangeId());
            } catch (Exception e) {
                log.error("Error while processing change: {}", change.getFullChangeId(), e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                destroy(change);
            }
        }, executorService);
    }

    private void addShutdownHoot() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    private Optional<String> getTopic(GerritChange change) {
        try {
            ChangeAttribute changeAttribute = change.getPatchSetEvent().change.get();
            return Optional.ofNullable(changeAttribute.topic);
        }
        catch (NullPointerException e) {
            return Optional.empty();
        }
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

    private boolean preProcessEvent(GerritChange change) {
        String eventType = Optional.ofNullable(change.getEventType()).orElse("");
        log.info("Event type {}", eventType);
        if (!EVENT_COMMENT_MAP.containsKey(eventType) ) {
            return false;
        }

        if (!isReviewEnabled(change)) {
            return false;
        }
        boolean isCommentEvent = EVENT_COMMENT_MAP.get(eventType);
        if (isCommentEvent) {
            if (!gerritClient.retrieveLastComments(change)) {
                if (settings.getForcedReview()) {
                    isCommentEvent = false;
                }
                else {
                    log.info("No comments found for review");
                    return false;
                }
            }
        }
        else {
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

    private void destroy(GerritChange change) {
        gerritClient.destroy(change);
        DynamicSettings.removeInstance(change);
    }

}
