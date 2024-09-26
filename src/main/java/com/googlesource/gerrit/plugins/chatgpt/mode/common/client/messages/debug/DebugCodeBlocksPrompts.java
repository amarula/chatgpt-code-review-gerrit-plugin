package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptClientStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.distanceCodeDelimiter;

public class DebugCodeBlocksPrompts extends DebugCodeBlocksComposer {
    private static final String PATCH_SET_PLACEHOLDER = "<PATCH_SET>";
    private static final String COMMIT_MESSAGE_PATCH_TEMPLATE = "Subject: <COMMIT_MESSAGE> Change-Id: ... " +
            PATCH_SET_PLACEHOLDER;

    private final Configuration config;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final LinkedHashMap<String, String> prompts = new LinkedHashMap<>();

    public DebugCodeBlocksPrompts(
            Localizer localizer,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change
    ) {
        super(localizer, "message.dump.prompts.title");
        this.config = config;
        this.change = change;
        this.changeSetData = changeSetData;
    }

    public String getDebugCodeBlock() {
        return super.getDebugCodeBlock(getPrompts());
    }

    private List<String> getPrompts() {
        switch (config.getGptMode()) {
            case stateful -> populateStatefulPrompts();
            case stateless -> populateStatelessPrompts();
        }
        return prompts.entrySet().stream()
                .map(e -> getAsTitle(e.getKey()) + "\n" + distanceCodeDelimiter(e.getValue()) + "\n")
                .collect(Collectors.toList());
    }

    private void populateStatefulPrompts() {
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, false);
        IChatGptPromptStateful chatGptPromptStateful = getChatGptPromptStateful(config, changeSetData, change);
        if (chatGptParameters.shouldSpecializeAssistants()) {
            prompts.put("ReviewCodePrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(PATCH_SET_PLACEHOLDER));
            changeSetData.setReviewAssistantStage(ChatGptClientStateful.ReviewAssistantStages.REVIEW_COMMIT_MESSAGE);
            chatGptPromptStateful = getChatGptPromptStateful(config, changeSetData, change);
            prompts.put("ReviewCommitMessagePrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
        }
        else {
            prompts.put("ReviewPrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
        }
    }

    private void populateStatelessPrompts() {
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, false);
        prompts.put("SystemPrompt", chatGptPromptStateless.getGptSystemPrompt());
        prompts.put("UserPrompt", chatGptPromptStateless.getGptUserPrompt(changeSetData, COMMIT_MESSAGE_PATCH_TEMPLATE));
    }
}
