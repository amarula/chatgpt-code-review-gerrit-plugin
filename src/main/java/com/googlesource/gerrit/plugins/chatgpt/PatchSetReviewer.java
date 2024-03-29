package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClientReview;
import com.googlesource.gerrit.plugins.chatgpt.client.messages.DebugMessages;
import com.googlesource.gerrit.plugins.chatgpt.client.patch.comment.GerritCommentRange;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.model.review.ReviewBatch;
import com.googlesource.gerrit.plugins.chatgpt.model.settings.Settings;
import com.googlesource.gerrit.plugins.chatgpt.settings.DynamicSettings;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class PatchSetReviewer {
    private static final String SPLIT_REVIEW_MSG = "Too many changes. Please consider splitting into patches smaller " +
            "than %s lines for review.";

    private final Gson gson = new Gson();
    private final GerritClient gerritClient;
    private final ChatGptClient chatGptClient;

    private Configuration config;
    private GerritCommentRange gerritCommentRange;
    private List<ReviewBatch> reviewBatches;
    private List<GerritComment> commentProperties;
    private List<Integer> reviewScores;

    @Inject
    PatchSetReviewer(GerritClient gerritClient, ChatGptClient chatGptClient) {
        this.gerritClient = gerritClient;
        this.chatGptClient = chatGptClient;
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
        DynamicSettings.update(config, change, gerritClient);

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
        ChatGptResponseContent reviewJson = gson.fromJson(reviewReply, ChatGptResponseContent.class);
        Settings settings = DynamicSettings.getInstance(change);
        for (ChatGptReplyItem replyItem : reviewJson.getReplies()) {
            String reply = replyItem.getReply();
            Integer score = replyItem.getScore();
            boolean isNotNegative = isNotNegativeReply(score);
            boolean isIrrelevant = isIrrelevantReply(replyItem);
            boolean isHidden = replyItem.isRepeated() || replyItem.isConflicting() || isIrrelevant || isNotNegative;
            if (!replyItem.isConflicting() && !isIrrelevant && score != null) {
                reviewScores.add(score);
            }
            if (settings.getReplyFilterEnabled() && isHidden) {
                continue;
            }
            if (settings.getDebugMode()) {
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
