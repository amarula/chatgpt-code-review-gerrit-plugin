package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptDataPrompt {
    private final IChatGptDataPrompt chatGptDataPromptHandler;

    public ChatGptDataPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        chatGptDataPromptHandler = ChatGptPromptFactory.getChatGptDataPrompt(
                config,
                changeSetData,
                change,
                gerritClientData,
                localizer
        );
        log.debug("ChatGptDataPrompt initialized for change: {}", change.getFullChangeId());
    }

    public String buildPrompt() {
        log.debug("Building data prompt for ChatGPT.");
        for (int i = 0; i < chatGptDataPromptHandler.getCommentProperties().size(); i++) {
            chatGptDataPromptHandler.addMessageItem(i);
            log.debug("Added message item to prompt for comment index: {}", i);
        }
        List<ChatGptMessageItem> messageItems = chatGptDataPromptHandler.getMessageItems();
        String promptJson = messageItems.isEmpty() ? "" : getGson().toJson(messageItems);
        log.debug("Final chatGPT prompt JSON: {}", promptJson);
        return promptJson;
    }
}
