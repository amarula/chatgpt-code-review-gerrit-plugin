package com.googlesource.gerrit.plugins.chatgpt;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.comment.GerritCommentRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.filename.FilenameSanitizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class PatchSetReviewer {
    private static final String SPLIT_REVIEW_MSG = "Too many changes. Please consider splitting into patches smaller " +
            "than %s lines for review.";

    private final Configuration config;
    private final GerritClient gerritClient;
    private final ChangeSetData changeSetData;
    private final Provider<GerritClientReview> clientReviewProvider;
    @Getter
    private final IChatGptClient chatGptClient;
    private final Localizer localizer;
    private final DebugCodeBlocksReview debugCodeBlocksReview;

    private GerritCommentRange gerritCommentRange;
    private List<ReviewBatch> reviewBatches;
    private List<GerritComment> commentProperties;
    private List<Integer> reviewScores;

    @Inject
    PatchSetReviewer(
            GerritClient gerritClient,
            Configuration config,
            ChangeSetData changeSetData,
            Provider<GerritClientReview> clientReviewProvider,
            IChatGptClient chatGptClient,
            Localizer localizer
    ) {
        this.config = config;
        this.gerritClient = gerritClient;
        this.changeSetData = changeSetData;
        this.clientReviewProvider = clientReviewProvider;
        this.chatGptClient = chatGptClient;
        this.localizer = localizer;
        debugCodeBlocksReview = new DebugCodeBlocksReview(localizer);
        log.debug("PatchSetReviewer initialized.");
    }

    public void review(GerritChange change) throws Exception {
        log.debug("Starting review process for change: {}", change.getFullChangeId());
        reviewBatches = new ArrayList<>();
        reviewScores = new ArrayList<>();
        commentProperties = gerritClient.getClientData(change).getCommentProperties();
        gerritCommentRange = new GerritCommentRange(gerritClient, change);
        String patchSet = gerritClient.getPatchSet(change);
        if (patchSet.isEmpty() && config.getGptMode() == Settings.Modes.STATELESS) {
            log.info("No file to review has been found in the PatchSet");
            return;
        }
        ChangeSetDataHandler.update(config, change, gerritClient, changeSetData, localizer);

        if (changeSetData.shouldRequestChatGptReview()) {
            ChatGptResponseContent reviewReply = null;
            try {
                reviewReply = getReviewReply(change, patchSet);
                log.debug("ChatGPT response: {}", reviewReply);
            }
            catch (OpenAiConnectionFailException e) {
                changeSetData.setReviewSystemMessage(localizer.getText("message.openai.connection.error"));
            }
            if (reviewReply != null) {
                retrieveReviewBatches(reviewReply, change);
            }
        }
        clientReviewProvider.get().setReview(change, reviewBatches, changeSetData, getReviewScore(change));
    }

    private void setCommentBatchMap(ReviewBatch batchMap, Integer batchID) {
        if (commentProperties != null && batchID < commentProperties.size()) {
            GerritComment commentProperty = commentProperties.get(batchID);
            if (commentProperty != null && (commentProperty.getLine() != null || commentProperty.getRange() != null)) {
                String id = commentProperty.getId();
                String filename = commentProperty.getFilename();
                Integer line = commentProperty.getLine();
                GerritCodeRange range = commentProperty.getRange();
                if (range != null) {
                    batchMap.setId(id);
                    batchMap.setFilename(filename);
                    batchMap.setLine(line);
                    batchMap.setRange(range);
                }
            }
        }
    }

    private void setPatchSetReviewBatchMap(ReviewBatch batchMap, ChatGptReplyItem replyItem) {
        Optional<GerritCodeRange> optGerritCommentRange = gerritCommentRange.getGerritCommentRange(replyItem);
        if (optGerritCommentRange.isPresent()) {
            GerritCodeRange gerritCodeRange = optGerritCommentRange.get();
            batchMap.setFilename(replyItem.getFilename());
            batchMap.setLine(gerritCodeRange.getStartLine());
            batchMap.setRange(gerritCodeRange);
        }
    }

    private void retrieveReviewBatches(ChatGptResponseContent reviewReply, GerritChange change) {
        FilenameSanitizer filenameSanitizer = new FilenameSanitizer(gerritClient, change);
        log.debug("Retrieving review batches for change: {}", change.getFullChangeId());
        if (reviewReply.getMessageContent() != null && !reviewReply.getMessageContent().isEmpty()) {
            reviewBatches.add(new ReviewBatch(reviewReply.getMessageContent()));
            log.debug("Added single message content to review batches.");
            return;
        }
        for (ChatGptReplyItem replyItem : reviewReply.getReplies()) {
            String reply = replyItem.getReply();
            Integer score = replyItem.getScore();
            boolean isNotNegative = isNotNegativeReply(score);
            boolean isIrrelevant = isIrrelevantReply(replyItem);
            boolean isHidden = replyItem.isRepeated() || replyItem.isConflicting() || isIrrelevant || isNotNegative;
            if (!replyItem.isConflicting() && !isIrrelevant && score != null) {
                log.debug("Score added: {}", score);
                reviewScores.add(score);
            }
            if (!change.getIsCommentEvent() && changeSetData.getReplyFilterEnabled() && isHidden) {
                continue;
            }
            if (changeSetData.getDebugReviewMode()) {
                reply += debugCodeBlocksReview.getDebugCodeBlock(replyItem, isHidden);
            }
            ReviewBatch batchMap = new ReviewBatch(reply);
            if (change.getIsCommentEvent() && replyItem.getId() != null) {
                setCommentBatchMap(batchMap, replyItem.getId());
            }
            else {
                filenameSanitizer.sanitizeFilename(replyItem);
                setPatchSetReviewBatchMap(batchMap, replyItem);
            }
            reviewBatches.add(batchMap);
            log.debug("Added review batch from reply item: {}", batchMap);
        }
    }

    private ChatGptResponseContent getReviewReply(GerritChange change, String patchSet) throws Exception {
        log.debug("Generating review reply for patch set.");
        List<String> patchLines = Arrays.asList(patchSet.split("\n"));
        if (patchLines.size() > config.getMaxReviewLines()) {
            log.warn("Patch set too large for review, size: {}, max allowed: {}", patchLines.size(),
                    config.getMaxReviewLines());
            return new ChatGptResponseContent(String.format(SPLIT_REVIEW_MSG, config.getMaxReviewLines()));
        }

        return chatGptClient.ask(changeSetData, change, patchSet);
    }

    private Integer getReviewScore(GerritChange change) {
        log.debug("Calculating review score for change ID: {}", change.getFullChangeId());
        if (config.isVotingEnabled()) {
            return change.getIsCommentEvent() ? null :
                    (reviewScores.isEmpty() ? 0 : Collections.min(reviewScores));
        }
        else {
            return null;
        }
    }

    private boolean isNotNegativeReply(Integer score) {
        return score != null &&
                config.getFilterNegativeComments() &&
                score >= config.getFilterCommentsBelowScore();
    }

    private boolean isIrrelevantReply(ChatGptReplyItem replyItem) {
        return config.getFilterRelevantComments() &&
                replyItem.getRelevance() != null &&
                replyItem.getRelevance() < config.getFilterCommentsRelevanceThreshold();
    }
}
