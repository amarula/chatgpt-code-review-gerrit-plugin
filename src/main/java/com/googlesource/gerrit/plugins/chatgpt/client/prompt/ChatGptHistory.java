package com.googlesource.gerrit.plugins.chatgpt.client.prompt;

import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.chatgpt.client.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.chatgpt.ChatGptRequest;
import com.googlesource.gerrit.plugins.chatgpt.model.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.model.common.CommentData;
import com.googlesource.gerrit.plugins.chatgpt.model.common.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.client.gerrit.GerritClientComments.GLOBAL_MESSAGES_FILENAME;

@Slf4j
public class ChatGptHistory extends ChatGptComment {
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final Gson gson = new Gson();
    private final HashMap<String, GerritComment> commentMap;
    private final HashMap<String, GerritComment> commentGlobalMap;
    private final List<GerritComment> detailComments;

    public ChatGptHistory(Configuration config, GerritChange change, GerritClientData gerritClientData) {
        super(config, change);
        detailComments = gerritClientData.getDetailComments() ;
        CommentData commentData = gerritClientData.getCommentData();
        commentMap = commentData.getCommentMap();
        commentGlobalMap = commentData.getCommentGlobalMap();
    }

    public String retrieveHistory(GerritComment commentProperty) {
        if (commentProperty.getFilename().equals(GLOBAL_MESSAGES_FILENAME) && detailComments != null) {
            return retrieveGlobalMessageHistory();
        }
        else {
            return retrieveMessageHistory(commentProperty);
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
            if (detailComment.isAutogenerated()) {
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
        String messageContent = getCleanedMessage(comment);
        if (messageContent.isEmpty()) {
            return;
        }
        ChatGptRequest.Message message = ChatGptRequest.Message.builder()
                .role(getRoleFromComment(comment))
                .content(messageContent)
                .build();
        messageHistory.add(message);
    }

}
