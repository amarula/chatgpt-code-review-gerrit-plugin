package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.*;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptDataPromptRequestsStateless;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptPromptFactory {

    public static IChatGptPromptStateful getChatGptPromptStateful(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change
    ) {
        if (change.getIsCommentEvent()) {
            log.info("ChatGptPromptFactory: Return ChatGptPromptStatefulRequests");
            return new ChatGptPromptStatefulRequests(config, changeSetData, change);
        }
        else {
            if (config.getGptReviewCommitMessages() && config.getTaskSpecificAssistants()) {
                return switch (changeSetData.getReviewAssistantStage()) {
                    case REVIEW_CODE -> {
                        log.info("ChatGptPromptFactory: Return ChatGptPromptStatefulReviewCode");
                        yield new ChatGptPromptStatefulReviewCode(config, changeSetData, change);
                    }
                    case REVIEW_COMMIT_MESSAGE -> {
                        log.info("ChatGptPromptFactory: Return ChatGptPromptStatefulReviewCommitMessage");
                        yield new ChatGptPromptStatefulReviewCommitMessage(config, changeSetData, change);
                    }
                };
            }
            else {
                log.info("ChatGptPromptFactory: Return ChatGptPromptStatefulReview for Unified Review");
                return new ChatGptPromptStatefulReview(config, changeSetData, change);
            }
        }
    }

    public static IChatGptDataPrompt getChatGptDataPrompt(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        if (change.getIsCommentEvent()) {
            if (config.getGptMode() == Settings.Modes.stateless) {
                log.info("ChatGptPromptFactory: Return ChatGptDataPromptRequestsStateless");
                return new ChatGptDataPromptRequestsStateless(config, changeSetData, gerritClientData, localizer);
            }
            else {
                log.info("ChatGptPromptFactory: Return ChatGptDataPromptRequestsStateful");
                return new ChatGptDataPromptRequestsStateful(config, changeSetData, gerritClientData, localizer);
            }
        }
        else {
            log.info("ChatGptPromptFactory: Return ChatGptDataPromptReview");
            return new ChatGptDataPromptReview(config, changeSetData, gerritClientData, localizer);
        }
    }
}
