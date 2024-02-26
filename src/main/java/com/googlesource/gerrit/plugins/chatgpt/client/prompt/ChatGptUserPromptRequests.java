package com.googlesource.gerrit.plugins.chatgpt.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.client.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.model.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.model.common.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ChatGptUserPromptRequests extends ChatGptUserPromptBase {
    public ChatGptUserPromptRequests(Configuration config, GerritChange change, GerritClientData gerritClientData) {
        super(config, change, gerritClientData);
        commentProperties = commentData.getCommentProperties();
    }

    public void addMessageItem(int i) {
        ChatGptMessageItem messageItem = getMessageItem(i);
        messageItem.setId(i);
        messageItems.add(messageItem);
    }

    protected ChatGptMessageItem getMessageItem(int i) {
        ChatGptMessageItem messageItem = super.getMessageItem(i);
        List<ChatGptRequestMessage> messageHistories = gptMessageHistory.retrieveHistory(commentProperties.get(i));
        ChatGptRequestMessage request = messageHistories.remove(messageHistories.size() -1);
        messageItem.setRequest(request.getContent());
        if (!messageHistories.isEmpty()) {
            messageItem.setHistory(messageHistories);
        }

        return messageItem;
    }

}
