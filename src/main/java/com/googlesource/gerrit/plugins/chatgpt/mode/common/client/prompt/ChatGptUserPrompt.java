package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
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
    private final ChatGptUserPromptBase chatGptUserPromptBase;

    public ChatGptUserPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        if (change.getIsCommentEvent()) {
            chatGptUserPromptBase = new ChatGptUserPromptRequests(config, changeSetData, gerritClientData, localizer);
        }
        else {
            chatGptUserPromptBase = new ChatGptUserPromptReview(config, changeSetData, gerritClientData, localizer);
        }
    }

    public String buildPrompt() {
        for (int i = 0; i < chatGptUserPromptBase.getCommentProperties().size(); i++) {
            chatGptUserPromptBase.addMessageItem(i);
        }
        List<ChatGptMessageItem> messageItems = chatGptUserPromptBase.getMessageItems();
        return messageItems.isEmpty() ? "" : getGson().toJson(messageItems);
    }

}
