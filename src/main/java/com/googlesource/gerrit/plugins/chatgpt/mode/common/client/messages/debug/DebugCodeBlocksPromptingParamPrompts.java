package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;

public class DebugCodeBlocksPromptingParamPrompts extends DebugCodeBlocksPromptingParamBase {
    private static final String PATCH_SET_PLACEHOLDER = "<PATCH_SET>";
    private static final String COMMIT_MESSAGE_PATCH_TEMPLATE = "Subject: <COMMIT_MESSAGE> Change-Id: ... " +
            PATCH_SET_PLACEHOLDER;

    private final Configuration config;
    private final ChangeSetData changeSetData;

    public DebugCodeBlocksPromptingParamPrompts(
            Localizer localizer,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(localizer, "message.dump.prompts.title", config, changeSetData, change, codeContextPolicy);
        this.config = config;
        this.changeSetData = changeSetData;
    }

    @Override
    protected void populateStatefulSpecializedCodeReviewParameters() {
        promptingParameters.put("ReviewCodePrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(PATCH_SET_PLACEHOLDER));
    }

    @Override
    protected void populateStatefulSpecializedCommitMessageReviewParameters() {
        promptingParameters.put("ReviewCommitMessagePrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
    }

    @Override
    protected void populateStatefulReviewParameters() {
        promptingParameters.put("ReviewPrompt", chatGptPromptStateful.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
    }

    @Override
    protected void populateStatelessParameters() {
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, false);
        promptingParameters.put("SystemPrompt", chatGptPromptStateless.getGptSystemPrompt());
        promptingParameters.put("UserPrompt", chatGptPromptStateless.getGptUserPrompt(changeSetData, COMMIT_MESSAGE_PATCH_TEMPLATE));
    }
}
