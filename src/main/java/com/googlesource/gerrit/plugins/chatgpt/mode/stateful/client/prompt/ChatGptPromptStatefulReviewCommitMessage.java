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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetHelper.filterCommitMessage;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

@Slf4j
public class ChatGptPromptStatefulReviewCommitMessage extends ChatGptPromptStatefulReview implements IChatGptPromptStateful {
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES_GUIDELINES;

    public ChatGptPromptStatefulReviewCommitMessage(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(config, changeSetData, change, codeContextPolicy);
        loadDefaultPrompts("promptsStatefulReviewCommitMessage");
        log.debug("Initialized ChatGptPromptStatefulReviewCommitMessage for project: {}", change.getProjectName());
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        instructions.addAll(List.of(
                joinWithNewLine(new ArrayList<>(List.of(
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES,
                        getGptAssistantInstructionsReview(false, true, false),
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES
                ))),
                DEFAULT_GPT_REVIEW_PROMPT_INSTRUCTIONS_COMMIT_MESSAGES,
                getPatchSetReviewPromptInstructions(),
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES_GUIDELINES,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES
        ));
        log.debug("Commit Message Review specific GPT Assistant Instructions added: {}", instructions);
    }

    @Override
    public String getDefaultGptThreadReviewMessage(String patchSet) {
        String filteredPatchSet = filterCommitMessage(patchSet);
        log.debug("Filtered Commit Message for Patch Set: {}", filteredPatchSet);
        return super.getDefaultGptThreadReviewMessage(filteredPatchSet);
    }
}
