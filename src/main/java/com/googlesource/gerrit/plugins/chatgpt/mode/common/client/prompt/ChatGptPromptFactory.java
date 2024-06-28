package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptDataPromptRequestsStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptDataPromptRequestsStateless;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptPromptFactory {

    public static IChatGptDataPrompt getChatGptDataPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        if (change.getIsCommentEvent()) {
            if ((config.getGptMode() == Settings.Modes.stateless)) {
                log.info("ChatGptPromptFactory: Returned ChatGptDataPromptRequestsStateless");
                return new ChatGptDataPromptRequestsStateless(config, changeSetData, gerritClientData, localizer);
            } else {
                log.info("ChatGptPromptFactory: Returned ChatGptDataPromptRequestsStateful");
                return new ChatGptDataPromptRequestsStateful(config, changeSetData, gerritClientData, localizer);
            }
        } else {
            log.info("ChatGptPromptFactory: Returned ChatGptDataPromptReview");
            return new ChatGptDataPromptReview(config, changeSetData, gerritClientData, localizer);
        }
    }
}
