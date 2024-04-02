package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptUserPromptReview extends ChatGptUserPromptBase {
    public ChatGptUserPromptReview(Configuration config, GerritChange change, GerritClientData gerritClientData) {
        super(config, change, gerritClientData);
        commentProperties = new ArrayList<>(commentData.getCommentMap().values());
    }

    public void addMessageItem(int i) {
        ChatGptMessageItem messageItem = getMessageItem(i);
        if (messageItem.getHistory() != null) {
            messageItems.add(messageItem);
        }
    }

    protected ChatGptMessageItem getMessageItem(int i) {
        ChatGptMessageItem messageItem = super.getMessageItem(i);
        List<ChatGptRequestMessage> messageHistories = gptMessageHistory.retrieveHistory(commentProperties.get(i),
                true);
        setHistories(messageItem, messageHistories);

        return messageItem;
    }

}
