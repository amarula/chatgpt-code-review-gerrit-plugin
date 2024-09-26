package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

public class DebugCodeBlocksPromptingParamInstructions extends DebugCodeBlocksPromptingParamBase {
    private final Localizer localizer;

    public DebugCodeBlocksPromptingParamInstructions(
            Localizer localizer,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change
    ) {
        super(localizer, "message.dump.instructions.title", config, changeSetData, change);
        this.localizer = localizer;
    }

    @Override
    protected void populateStatefulSpecializedCodeReviewParameters() {
        promptingParameters.put("AssistantCodeInstructions", chatGptPromptStateful.getDefaultGptAssistantInstructions());
    }

    @Override
    protected void populateStatefulSpecializedCommitMessageReviewParameters() {
        promptingParameters.put("AssistantCommitMessageInstructions", chatGptPromptStateful.getDefaultGptAssistantInstructions());
    }

    @Override
    protected void populateStatefulReviewParameters() {
        promptingParameters.put("AssistantInstructions", chatGptPromptStateful.getDefaultGptAssistantInstructions());
    }

    @Override
    protected void populateStatelessParameters() {
        promptingParameters.put("AssistantInstructions", localizer.getText("message.dump.instructions.mode.mismatch"));
    }
}
