package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptUserPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptUserPrompt {
    private final IChatGptUserPrompt chatGptUserPromptHandler;

    public ChatGptUserPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        chatGptUserPromptHandler = ChatGptUserPromptFactory.getChatGptUserPrompt(
                config,
                changeSetData,
                change,
                gerritClientData,
                localizer
        );
    }

    public String buildPrompt() {
        for (int i = 0; i < chatGptUserPromptHandler.getCommentProperties().size(); i++) {
            chatGptUserPromptHandler.addMessageItem(i);
        }
        List<ChatGptMessageItem> messageItems = chatGptUserPromptHandler.getMessageItems();
        return messageItems.isEmpty() ? "" : getGson().toJson(messageItems);
    }
}
