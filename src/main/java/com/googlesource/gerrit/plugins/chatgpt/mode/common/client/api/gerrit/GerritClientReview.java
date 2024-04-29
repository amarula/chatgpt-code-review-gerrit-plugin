package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gerrit.extensions.client.Comment;
import com.google.gerrit.entities.LabelId;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewInput.CommentInput;
import com.google.gerrit.extensions.api.changes.ReviewResult;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.MessageSanitizer.sanitizeChatGptMessage;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.EMPTY_REVIEW_MESSAGE;

@Slf4j
public class GerritClientReview extends GerritClientAccount {
    @VisibleForTesting
    @Inject
    public GerritClientReview(Configuration config, AccountCache accountCache) {
        super(config, accountCache);
    }

    public void setReview(GerritChange change, List<ReviewBatch> reviewBatches, Integer reviewScore) throws Exception {
        ReviewInput reviewInput = buildReview(reviewBatches, reviewScore);
        if (reviewInput.comments == null && reviewInput.message == null) {
            return;
        }
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            ReviewResult result = config
                .getGerritApi()
                .changes()
                .id(
                    change.getProjectName(),
                    change.getBranchNameKey().shortName(),
                    change.getChangeKey().get())
                .current()
                .review(reviewInput);

            if (!Strings.isNullOrEmpty(result.error)) {
              log.error("Review setting failed with status code: {}", result.error);
            }
        }
    }

    public void setReview(GerritChange change, List<ReviewBatch> reviewBatches) throws Exception {
        setReview(change, reviewBatches, null);
    }

    private ReviewInput buildReview(List<ReviewBatch> reviewBatches, Integer reviewScore) {
        ReviewInput reviewInput = ReviewInput.create();
        Map<String, List<CommentInput>> comments = new HashMap<>();
        for (ReviewBatch reviewBatch : reviewBatches) {
            String message = sanitizeChatGptMessage(reviewBatch.getContent());
            if (message.trim().isEmpty()) {
                log.info("Empty message from review not submitted.");
                continue;
            }
            boolean unresolved;
            String filename = reviewBatch.getFilename();
            List<CommentInput> filenameComments = comments.getOrDefault(filename, new ArrayList<>());
            CommentInput filenameComment = new CommentInput();
            filenameComment.message = message;
            if (reviewBatch.getLine() != null || reviewBatch.getRange() != null) {
                filenameComment.line = reviewBatch.getLine();
                Optional.ofNullable(reviewBatch.getRange())
                    .ifPresent(
                        r -> {
                          Comment.Range range = new Comment.Range();
                          range.startLine = r.startLine;
                          range.startCharacter = r.startCharacter;
                          range.endLine = r.endLine;
                          range.endCharacter = r.endCharacter;
                          filenameComment.range = range;
                        });
                filenameComment.inReplyTo = reviewBatch.getId();
                unresolved = !config.getInlineCommentsAsResolved();
            }
            else {
                unresolved = !config.getPatchSetCommentsAsResolved();
            }
            filenameComment.unresolved = unresolved;
            filenameComments.add(filenameComment);
            comments.putIfAbsent(filename, filenameComments);
        }
        if (comments.isEmpty()) {
            reviewInput.message(EMPTY_REVIEW_MESSAGE);
        }
        else {
            reviewInput.comments = comments;
        }
        if (reviewScore != null) {
            reviewInput.label(LabelId.CODE_REVIEW, reviewScore);
        }
        return reviewInput;
    }

}
