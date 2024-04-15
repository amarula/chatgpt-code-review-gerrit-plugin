package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gson.reflect.TypeToken;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.ClientMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.CommentData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils.getTimeStamp;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;

@Slf4j
public class GerritClientComments extends GerritClientAccount {
    private static final Integer MAX_SECS_GAP_BETWEEN_EVENT_AND_COMMENT = 2;

    private final HashMap<String, GerritComment> commentMap;
    private final HashMap<String, GerritComment> patchSetCommentMap;

    private String authorUsername;
    @Getter
    private List<GerritComment> commentProperties;

    public GerritClientComments(Configuration config) {
        super(config);
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
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritGetAllPatchSetCommentsUri(change.getFullChangeId()));
        String responseBody = forwardGetRequest(uri);
        Type mapEntryType = new TypeToken<Map<String, List<GerritComment>>>(){}.getType();
        Map<String, List<GerritComment>> lastCommentMap = getGson().fromJson(responseBody, mapEntryType);

        String latestChangeMessageId = null;
        HashMap<String, List<GerritComment>> latestComments = new HashMap<>();
        for (Map.Entry<String, List<GerritComment>> entry : lastCommentMap.entrySet()) {
            String filename = entry.getKey();
            log.info("Commented filename: {}", filename);

            List<GerritComment> commentsArray = entry.getValue();

            for (GerritComment commentObject : commentsArray) {
                commentObject.setFilename(filename);
                String commentId = commentObject.getId();
                String changeMessageId = commentObject.getChangeMessageId();
                String commentAuthorUsername = commentObject.getAuthor().getUsername();
                log.debug("Change Message Id: {} - Author: {}", latestChangeMessageId, commentAuthorUsername);
                long updatedTimeStamp = getTimeStamp(commentObject.getUpdated());
                if (commentAuthorUsername.equals(authorUsername) &&
                        updatedTimeStamp >= change.getEventTimeStamp() - MAX_SECS_GAP_BETWEEN_EVENT_AND_COMMENT) {
                    log.debug("Found comment with updatedTimeStamp : {}", updatedTimeStamp);
                    latestChangeMessageId = changeMessageId;
                }
                latestComments.computeIfAbsent(changeMessageId, k -> new ArrayList<>()).add(commentObject);
                commentMap.put(commentId, commentObject);
                if (filename.equals(GERRIT_PATCH_SET_FILENAME)) {
                    patchSetCommentMap.put(changeMessageId, commentObject);
                }
            }
        }

        return latestComments.getOrDefault(latestChangeMessageId, null);
    }

    private void addLastComments(GerritChange change) {
        ClientMessage clientMessage = new ClientMessage(config, change);
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

}
