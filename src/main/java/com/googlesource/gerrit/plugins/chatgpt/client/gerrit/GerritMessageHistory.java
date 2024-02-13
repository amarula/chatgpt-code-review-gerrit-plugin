package com.googlesource.gerrit.plugins.chatgpt.client.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.client.model.chatGpt.ChatGptRequest;
import com.googlesource.gerrit.plugins.chatgpt.client.model.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class GerritMessageHistory extends GerritMessageComment {
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final HashMap<String, GerritComment> commentMap;
    private final HashMap<String, GerritComment> commentGlobalMap;
    private final List<GerritComment> detailComments;

    public GerritMessageHistory(Configuration config, GerritChange change, HashMap<String, GerritComment> commentMap,
                                HashMap<String, GerritComment> commentGlobalMap, List<GerritComment> detailComments) {
        super(config, change);
        this.commentMap = commentMap;
        this.commentGlobalMap = commentGlobalMap;
        this.detailComments = detailComments;
    }

    public String retrieveCommentMessage(GerritComment commentProperty) {
        if (commentProperty.getInReplyTo() != null) {
            return retrieveMessageHistory(commentProperty);
        }
        else if (commentProperty.getFilename().equals(GLOBAL_MESSAGES_FILENAME) && detailComments != null) {
            return retrieveGlobalMessageHistory();
        }
        else {
            return getMessageWithoutMentions(commentProperty);
        }
    }

    private String getRoleFromComment(GerritComment currentComment) {
        return isFromAssistant(currentComment) ? ROLE_ASSISTANT : ROLE_USER;
    }

    private String retrieveMessageHistory(GerritComment currentComment) {
        List<ChatGptRequest.Message> messageHistory = new ArrayList<>();
        while (currentComment != null) {
            addMessageToHistory(messageHistory, currentComment);
            currentComment = commentMap.get(currentComment.getInReplyTo());
        }
        // Reverse the history sequence so that the oldest message appears first and the newest message is last
        Collections.reverse(messageHistory);

        return gson.toJson(messageHistory);
    }

    private String retrieveGlobalMessageHistory() {
        List<ChatGptRequest.Message> messageHistory = new ArrayList<>();
        for (GerritComment detailComment : detailComments) {
            if (isAutogenerated(detailComment)) {
                continue;
            }
            if (!isFromAssistant(detailComment)) {
                GerritComment patchSetLevelMessage = commentGlobalMap.get(detailComment.getId());
                if (patchSetLevelMessage != null) {
                    detailComment = patchSetLevelMessage;
                }
            }
            addMessageToHistory(messageHistory, detailComment);
        }
        return gson.toJson(messageHistory);
    }

    private void addMessageToHistory(List<ChatGptRequest.Message> messageHistory, GerritComment comment) {
        ChatGptRequest.Message message = ChatGptRequest.Message.builder()
                .role(getRoleFromComment(comment))
                .content(getCleanedMessage(comment))
                .build();
        messageHistory.add(message);
    }

}
