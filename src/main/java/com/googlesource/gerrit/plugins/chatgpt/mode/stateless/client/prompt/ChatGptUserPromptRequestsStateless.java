package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptUserPromptRequests;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.prompt.IChatGptUserPrompt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptUserPromptRequestsStateless extends ChatGptUserPromptRequests implements IChatGptUserPrompt {
    public ChatGptUserPromptRequestsStateless(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
    }

    protected ChatGptMessageItem getMessageItem(int i) {
        super.getMessageItem(i);
        setHistory(messageItem, messageHistory);

        return messageItem;
    }
}
