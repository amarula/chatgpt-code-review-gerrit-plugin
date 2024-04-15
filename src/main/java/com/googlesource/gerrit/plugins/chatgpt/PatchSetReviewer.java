package com.googlesource.gerrit.plugins.chatgpt;

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.ModeClassLoader;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.DebugMessages;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.comment.GerritCommentRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.chatgpt.ChatGptClientStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt.IChatGptClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ClassUtils.registerDynamicClasses;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class PatchSetReviewer {
    private static final String SPLIT_REVIEW_MSG = "Too many changes. Please consider splitting into patches smaller " +
            "than %s lines for review.";

    private final GerritClient gerritClient;

    private Configuration config;
    @Getter
    private IChatGptClient chatGptClient;
    private GerritCommentRange gerritCommentRange;
    private List<ReviewBatch> reviewBatches;
    private List<GerritComment> commentProperties;
    private List<Integer> reviewScores;

    @Inject
    PatchSetReviewer(GerritClient gerritClient) {
        this.gerritClient = gerritClient;
    }

    public void review(Configuration config, GerritChange change) throws Exception {
        this.config = config;
        reviewBatches = new ArrayList<>();
        reviewScores = new ArrayList<>();
        commentProperties = gerritClient.getClientData(change).getCommentProperties();
        gerritCommentRange = new GerritCommentRange(gerritClient, change);
        GerritClientReview gerritClientReview = new GerritClientReview(config);
        String patchSet = gerritClient.getPatchSet(change);
        if (patchSet.isEmpty()) {
            log.info("No file to review has been found in the PatchSet");
            return;
        }
        ChangeSetDataHandler.update(config, change, gerritClient);

        String reviewReply = getReviewReply(change, patchSet);
        log.debug("ChatGPT response: {}", reviewReply);

        retrieveReviewBatches(reviewReply, change);
        gerritClientReview.setReview(change.getFullChangeId(), reviewBatches, getReviewScore());
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

    private void retrieveReviewBatches(String reviewReply, GerritChange change) {
        ChatGptResponseContent reviewJson = getGson().fromJson(reviewReply, ChatGptResponseContent.class);
        ChangeSetData changeSetData = ChangeSetDataHandler.getInstance(change);
        for (ChatGptReplyItem replyItem : reviewJson.getReplies()) {
            String reply = replyItem.getReply();
            Integer score = replyItem.getScore();
            boolean isNotNegative = isNotNegativeReply(score);
            boolean isIrrelevant = isIrrelevantReply(replyItem);
            boolean isHidden = replyItem.isRepeated() || replyItem.isConflicting() || isIrrelevant || isNotNegative;
            if (!replyItem.isConflicting() && !isIrrelevant && score != null) {
                reviewScores.add(score);
            }
            if (changeSetData.getReplyFilterEnabled() && isHidden) {
                continue;
            }
            if (changeSetData.getDebugMode()) {
                reply += DebugMessages.getDebugMessage(replyItem, isHidden);
            }
            ReviewBatch batchMap = new ReviewBatch();
            batchMap.setContent(reply);
            if (change.getIsCommentEvent() && replyItem.getId() != null) {
                setCommentBatchMap(batchMap, replyItem.getId());
            }
            else {
                setPatchSetReviewBatchMap(batchMap, replyItem);
            }
            reviewBatches.add(batchMap);
        }
    }

    private String getReviewReply(GerritChange change, String patchSet) throws Exception {
        List<String> patchLines = Arrays.asList(patchSet.split("\n"));
        if (patchLines.size() > config.getMaxReviewLines()) {
            log.warn("Patch set too large. Skipping review. changeId: {}", change.getFullChangeId());
            return String.format(SPLIT_REVIEW_MSG, config.getMaxReviewLines());
        }
        chatGptClient = (IChatGptClient) ModeClassLoader.getInstance(
                "client.api.chatgpt.ChatGptClient", config);
        registerDynamicClasses(ChatGptClientStateless.class);

        return chatGptClient.ask(config, change, patchSet);
    }

    private Integer getReviewScore() {
        if (config.isVotingEnabled()) {
            return reviewScores.isEmpty() ? 0 : Collections.min(reviewScores);
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
