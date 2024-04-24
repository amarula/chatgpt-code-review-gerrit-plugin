package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ChatGptUserPromptRequests extends ChatGptUserPromptBase {
    public ChatGptUserPromptRequests(Configuration config, ChangeSetData changeSetData, GerritChange change, GerritClientData gerritClientData) {
        super(config, changeSetData, change, gerritClientData);
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
        setHistories(messageItem, messageHistories);

        return messageItem;
    }

}
