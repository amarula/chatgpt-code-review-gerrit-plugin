package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.googlesource.gerrit.plugins.chatgpt.PatchSetReviewer;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.listener.IEventHandlerType;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventHandlerTypeCommentAdded implements IEventHandlerType {
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final PatchSetReviewer reviewer;
    private final GerritClient gerritClient;

    EventHandlerTypeCommentAdded(
            ChangeSetData changeSetData,
            GerritChange change,
            PatchSetReviewer reviewer,
            GerritClient gerritClient
    ) {
        this.changeSetData = changeSetData;
        this.change = change;
        this.reviewer = reviewer;
        this.gerritClient = gerritClient;
        log.debug("Initialized EventHandlerTypeCommentAdded for full change ID: {}", change.getFullChangeId());
    }

    @Override
    public PreprocessResult preprocessEvent() {
        log.debug("Starting preprocessing event for comment added on change ID: {}", change.getFullChangeId());
        if (!gerritClient.retrieveLastComments(change)) {
            log.debug("No new comments found for full change ID: {}", change.getFullChangeId());
            if (changeSetData.getForcedReview()) {
                log.info("Forcing review due to settings for full change ID: {}", change.getFullChangeId());
                return PreprocessResult.SWITCH_TO_PATCH_SET_CREATED;
            }
            else if(changeSetData.getReviewSystemMessage() != null) {
                log.info("Echoing system message in the UI");
                return PreprocessResult.OK;
            }
            else {
                log.info("Exiting preprocessing as no comments require action for full change ID: {}",
                        change.getFullChangeId());
                return PreprocessResult.EXIT;
            }
        }
        else {
            log.debug("Comments retrieved during preprocessing for full change ID: {}", change.getFullChangeId());
        }
        change.setIsCommentEvent(true);
        return PreprocessResult.OK;
    }

    @Override
    public void processEvent() throws Exception {
        log.debug("Processing event to review comments on full change ID: {}", change.getFullChangeId());
        reviewer.review(change);
        log.debug("Completed processing event for reviewing comments on full change ID: {}", change.getFullChangeId());
    }
}
