package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptDataPromptRequests;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptDataPromptRequestsStateful extends ChatGptDataPromptRequests implements IChatGptDataPrompt {
    private static final String REPLY_MESSAGE_REFERENCE = " (Ref. message \"%s\")";

    public ChatGptDataPromptRequestsStateful(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
        log.debug("ChatGptDataPromptRequestsStateful initialized");
    }

    @Override
    protected ChatGptMessageItem getMessageItem(int i) {
        log.debug("Getting stateful Message Item");
        super.getMessageItem(i);
        if (!messageHistory.isEmpty()) {
            log.debug("Message History found: {}", messageHistory);
            messageItem.appendToRequest(getReferenceToLastMessage());
        }

        return messageItem;
    }

    private String getReferenceToLastMessage() {
        String referenceToLastMessage = String.format(REPLY_MESSAGE_REFERENCE,
                messageHistory.get(messageHistory.size() - 1).getContent());
        log.debug("Reference to last message: {}", referenceToLastMessage);
        return referenceToLastMessage;
    }
}
