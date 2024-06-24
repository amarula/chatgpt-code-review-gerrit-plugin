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

    }

    @Override
    public PreprocessResult preprocessEvent() {
        if (!gerritClient.retrieveLastComments(change)) {
            if (changeSetData.getForcedReview() || changeSetData.getForceDisplaySystemMessage()) {
                return PreprocessResult.SWITCH_TO_PATCH_SET_CREATED;
            } else {
                log.info("No comments found for review");
                return PreprocessResult.EXIT;
            }
        }
        change.setIsCommentEvent(true);

        return PreprocessResult.OK;
    }

    @Override
    public void processEvent() throws Exception {
        reviewer.review(change);
    }
}
