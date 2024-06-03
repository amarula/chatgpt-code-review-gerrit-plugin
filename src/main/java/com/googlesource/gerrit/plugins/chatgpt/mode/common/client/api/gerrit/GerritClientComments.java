package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.ClientMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.CommentData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils.getTimeStamp;
import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientDetail.toAuthor;
import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientDetail.toDateString;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;

@Slf4j
public class GerritClientComments extends GerritClientAccount {
    private static final Integer MAX_SECS_GAP_BETWEEN_EVENT_AND_COMMENT = 2;

    private final ChangeSetData changeSetData;
    private final HashMap<String, GerritComment> commentMap;
    private final HashMap<String, GerritComment> patchSetCommentMap;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final Localizer localizer;

    private String authorUsername;
    @Getter
    private List<GerritComment> commentProperties;

    @VisibleForTesting
    @Inject
    public GerritClientComments(
            Configuration config,
            AccountCache accountCache,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config, accountCache);
        this.changeSetData = changeSetData;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        this.localizer = localizer;
        commentProperties = new ArrayList<>();
        commentMap = new HashMap<>();
        patchSetCommentMap = new HashMap<>();
    }

    public CommentData getCommentData() {
        return new CommentData(commentProperties, commentMap, patchSetCommentMap);
    }

    public boolean retrieveLastComments(GerritChange change) {
        CommentAddedEvent commentAddedEvent = (CommentAddedEvent) change.getEvent();
        authorUsername = commentAddedEvent.author.get().username;
        log.debug("Found comments by '{}' on {}", authorUsername, change.getEventTimeStamp());
        if (authorUsername.equals(config.getGerritUserName())) {
            log.debug("These are the Bot's own comments, do not process them.");
            return false;
        }
        if (isDisabledUser(authorUsername)) {
            log.info("Review of comments from user '{}' is disabled.", authorUsername);
            return false;
        }
        addLastComments(change);

        return !commentProperties.isEmpty();
    }

    public void retrieveAllComments(GerritChange change) {
        try {
            retrieveComments(change);
        } catch (Exception e) {
            log.error("Error while retrieving all comments for change: {}", change.getFullChangeId(), e);
        }
    }

    private List<GerritComment> retrieveComments(GerritChange change) throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            Map<String, List<CommentInfo>> comments =
                config
                    .getGerritApi()
                    .changes()
                    .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                    .commentsRequest()
                    .get();

            // note that list of Map.Entry was used in order to keep the original response order
            List<Map.Entry<String, List<GerritComment>>> lastCommentEntries =
                comments.entrySet().stream()
                    .map(
                        entry ->
                            Map.entry(
                                entry.getKey(),
                                entry.getValue().stream()
                                    .map(GerritClientComments::toComment)
                                    .collect(toList())))
                    .collect(toList());

            String latestChangeMessageId = null;
            HashMap<String, List<GerritComment>> latestComments = new HashMap<>();
            for (Map.Entry<String, List<GerritComment>> entry : lastCommentEntries) {
                String filename = entry.getKey();
                log.info("Commented filename: {}", filename);

                List<GerritComment> commentsArray = entry.getValue();

                for (GerritComment commentObject : commentsArray) {
                    commentObject.setFilename(filename);
                    String commentId = commentObject.getId();
                    String changeMessageId = commentObject.getChangeMessageId();
                    String commentAuthorUsername = commentObject.getAuthor().getUsername();
                    log.debug(
                        "Change Message Id: {} - Author: {}", latestChangeMessageId, commentAuthorUsername);
                    long updatedTimeStamp = getTimeStamp(commentObject.getUpdated());
                    if (commentAuthorUsername.equals(authorUsername)
                        && updatedTimeStamp
                            >= change.getEventTimeStamp() - MAX_SECS_GAP_BETWEEN_EVENT_AND_COMMENT) {
                      log.debug("Found comment with updatedTimeStamp : {}", updatedTimeStamp);
                      latestChangeMessageId = changeMessageId;
                    }
                    latestComments
                        .computeIfAbsent(changeMessageId, k -> new ArrayList<>())
                        .add(commentObject);
                    commentMap.put(commentId, commentObject);
                    if (filename.equals(GERRIT_PATCH_SET_FILENAME)) {
                        patchSetCommentMap.put(changeMessageId, commentObject);
                    }
                }
            }

            return latestComments.getOrDefault(latestChangeMessageId, null);
        }
    }

    private void addLastComments(GerritChange change) {
        ClientMessage clientMessage = new ClientMessage(config, changeSetData, pluginDataHandlerProvider, localizer);
        try {
            List<GerritComment> latestComments = retrieveComments(change);
            if (latestComments == null) {
                return;
            }
            for (GerritComment latestComment : latestComments) {
                String commentMessage = latestComment.getMessage();
                if (clientMessage.parseCommands(commentMessage, true)) {
                    if (clientMessage.isContainingHistoryCommand()) {
                        clientMessage.processHistoryCommand();
                    }
                    commentProperties.clear();
                    return;
                }
                if (clientMessage.isBotAddressed(commentMessage)) {
                    commentProperties.add(latestComment);
                }
            }
        } catch (Exception e) {
            log.error("Error while retrieving last comments for change: {}", change.getFullChangeId(), e);
        }
    }

    private static GerritComment toComment(CommentInfo comment) {
        GerritComment gerritComment = new GerritComment();
        gerritComment.setAuthor(toAuthor(comment.author));
        gerritComment.setChangeMessageId(comment.changeMessageId);
        gerritComment.setUnresolved(comment.unresolved);
        gerritComment.setPatchSet(comment.patchSet);
        gerritComment.setId(comment.id);
        gerritComment.setTag(comment.tag);
        gerritComment.setLine(comment.line);
        Optional.ofNullable(comment.range)
            .ifPresent(
                range ->
                    gerritComment.setRange(
                        GerritCodeRange.builder()
                            .startLine(range.startLine)
                            .endLine(range.endLine)
                            .startCharacter(range.startCharacter)
                            .endCharacter(range.endCharacter)
                            .build()));
        gerritComment.setInReplyTo(comment.inReplyTo);
        Optional.ofNullable(comment.updated)
            .ifPresent(updated -> gerritComment.setUpdated(toDateString(updated)));
        gerritComment.setMessage(comment.message);
        gerritComment.setCommitId(comment.commitId);
        return gerritComment;
    }
}
