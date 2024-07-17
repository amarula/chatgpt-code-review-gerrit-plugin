package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptDataPromptReview extends ChatGptDataPromptBase implements IChatGptDataPrompt {
    public ChatGptDataPromptReview(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
        commentProperties = new ArrayList<>(commentData.getCommentMap().values());
        log.debug("ChatGptDataPromptReview initialized with comment properties.");
    }

    @Override
    public void addMessageItem(int i) {
        log.debug("Adding message item for review at index: {}", i);
        ChatGptMessageItem messageItem = getMessageItem(i);
        if (messageItem.getHistory() != null) {
            messageItems.add(messageItem);
            log.debug("Message item added with history: {}", messageItem);
        } else {
            log.debug("Message item not added due to empty history at index: {}", i);
        }
    }

    @Override
    protected ChatGptMessageItem getMessageItem(int i) {
        log.debug("Retrieving message item for review at index: {}", i);
        ChatGptMessageItem messageItem = super.getMessageItem(i);
        List<ChatGptRequestMessage> messageHistory = gptMessageHistory.retrieveHistory(commentProperties.get(i), true);
        setHistory(messageItem, messageHistory);
        log.debug("Message item populated with history for review: {}", messageItem);
        return messageItem;
    }
}
