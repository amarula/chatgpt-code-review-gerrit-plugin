/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

public class DebugCodeBlocksPromptingParamInstructions extends DebugCodeBlocksPromptingParamBase {
    private final Localizer localizer;

    public DebugCodeBlocksPromptingParamInstructions(
            Localizer localizer,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(localizer, "message.dump.instructions.title", config, changeSetData, change, codeContextPolicy);
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
