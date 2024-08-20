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
import com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfigManager;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.MessageSanitizer.sanitizeChatGptMessage;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithDoubleNewLine;

@Slf4j
public class GerritClientReview extends GerritClientAccount {
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final Localizer localizer;
    private final DebugCodeBlocksDynamicConfiguration debugCodeBlocksDynamicConfiguration;

    @VisibleForTesting
    @Inject
    public GerritClientReview(
            Configuration config,
            AccountCache accountCache,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config, accountCache);
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        this.localizer = localizer;
        debugCodeBlocksDynamicConfiguration = new DebugCodeBlocksDynamicConfiguration(localizer);
        log.debug("GerritClientReview initialized.");
    }

    public void setReview(
            GerritChange change,
            List<ReviewBatch> reviewBatches,
            ChangeSetData changeSetData,
            Integer reviewScore
    ) throws Exception {
        log.debug("Setting review for change ID: {}", change.getFullChangeId());
        ReviewInput reviewInput = buildReview(reviewBatches, changeSetData, reviewScore);
        if (reviewInput.comments == null && reviewInput.message == null) {
            log.debug("No comments or messages to post for review.");
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

    public void setReview(
            GerritChange change,
            List<ReviewBatch> reviewBatches,
            ChangeSetData changeSetData
    ) throws Exception {
        setReview(change, reviewBatches, changeSetData, null);
    }

    private ReviewInput buildReview(List<ReviewBatch> reviewBatches, ChangeSetData changeSetData, Integer reviewScore) {
        log.debug("Building review input.");
        ReviewInput reviewInput = ReviewInput.create();
        Map<String, List<CommentInput>> comments = new HashMap<>();
        String systemMessage = localizer.getText("message.empty.review");
        if (changeSetData.getReviewSystemMessage() != null) {
            systemMessage = changeSetData.getReviewSystemMessage();
        }
        else if (!changeSetData.shouldHideChatGptReview()) {
            comments = getReviewComments(reviewBatches);
            if (reviewScore != null) {
                reviewInput.label(LabelId.CODE_REVIEW, reviewScore);
            }
        }
        updateSystemMessage(reviewInput, comments.isEmpty(), systemMessage);
        if (!comments.isEmpty()) {
            reviewInput.comments = comments;
        }
        return reviewInput;
    }

    private void updateSystemMessage(ReviewInput reviewInput, boolean emptyComments, String systemMessage) {
        List<String> messages = new ArrayList<>();
        Map<String, String> dynamicConfig = new DynamicConfigManager(pluginDataHandlerProvider).getDynamicConfig();
        if (dynamicConfig != null && !dynamicConfig.isEmpty()) {
            messages.add(debugCodeBlocksDynamicConfiguration.getDebugCodeBlock(dynamicConfig));
        }
        if (emptyComments) {
            messages.add(localizer.getText("system.message.prefix") + ' ' + systemMessage);
        }
        if (!messages.isEmpty()) {
            reviewInput.message(joinWithDoubleNewLine(messages));
        }
        log.debug("System message for review set.");
    }

    private Map<String, List<CommentInput>> getReviewComments(List<ReviewBatch> reviewBatches) {
        log.debug("Getting review comments.");
        Map<String, List<CommentInput>> comments = new HashMap<>();
        for (ReviewBatch reviewBatch : reviewBatches) {
            String message = sanitizeChatGptMessage(reviewBatch.getContent());
            if (message.trim().isEmpty()) {
                log.info("Empty message from review not submitted for batch with ID: {}", reviewBatch.getId());
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
                        .ifPresent(r -> {
                            Comment.Range range = new Comment.Range();
                            range.startLine = r.startLine;
                            range.startCharacter = r.startCharacter;
                            range.endLine = r.endLine;
                            range.endCharacter = r.endCharacter;
                            filenameComment.range = range;
                            log.debug("Setting range for comment on file '{}': startLine {}, endLine {}",
                                    filename, range.startLine, range.endLine);
                        });
                unresolved = !config.getInlineCommentsAsResolved();
                log.debug("Comment for file '{}' is marked as unresolved: {}", filename, unresolved);
            }
            else {
                unresolved = !config.getPatchSetCommentsAsResolved();
                log.debug("Patch set comment for file '{}' is marked as unresolved: {}", filename, unresolved);
            }
            filenameComment.inReplyTo = reviewBatch.getId();
            filenameComment.unresolved = unresolved;
            filenameComments.add(filenameComment);
            comments.putIfAbsent(filename, filenameComments);
        }
        log.debug("Review comments processed.");
        return comments;
    }
}
