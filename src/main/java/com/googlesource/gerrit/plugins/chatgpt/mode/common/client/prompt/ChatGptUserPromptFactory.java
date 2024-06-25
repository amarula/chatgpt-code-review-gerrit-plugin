package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptUserPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptUserPromptRequestsStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptUserPromptRequestsStateless;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptUserPromptFactory {

    public static IChatGptUserPrompt getChatGptUserPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        if (change.getIsCommentEvent()) {
            if ((config.getGptMode() == Settings.Modes.stateless)) {
                log.info("ChatGptUserPromptFactory: Returned ChatGptUserPromptRequestsStateless");
                return new ChatGptUserPromptRequestsStateless(config, changeSetData, gerritClientData, localizer);
            } else {
                log.info("ChatGptUserPromptFactory: Returned ChatGptUserPromptRequestsStateful");
                return new ChatGptUserPromptRequestsStateful(config, changeSetData, gerritClientData, localizer);
            }
        } else {
            log.info("ChatGptUserPromptFactory: Returned ChatGptUserPromptReview");
            return new ChatGptUserPromptReview(config, changeSetData, gerritClientData, localizer);
        }
    }
}
