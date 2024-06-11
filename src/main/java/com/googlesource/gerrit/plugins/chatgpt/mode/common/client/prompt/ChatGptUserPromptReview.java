package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.prompt.IChatGptUserPrompt;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptUserPromptReview extends ChatGptUserPromptBase implements IChatGptUserPrompt {
    public ChatGptUserPromptReview(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
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
        List<ChatGptRequestMessage> messageHistory = gptMessageHistory.retrieveHistory(commentProperties.get(i),
                true);
        setHistory(messageItem, messageHistory);

        return messageItem;
    }
}
