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

package com.googlesource.gerrit.plugins.chatgpt.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;

public class DebugCodeBlocksPromptingParamInstructions extends DebugCodeBlocksPromptingParamBase {

  public DebugCodeBlocksPromptingParamInstructions(
      Localizer localizer,
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(
        localizer,
        "message.dump.instructions.title",
        config,
        changeSetData,
        change,
        codeContextPolicy);
  }

  @Override
  protected void populateSpecializedCodeReviewParameters() {
    promptingParameters.put(
        "AssistantCodeInstructions", chatGptPrompt.getDefaultGptAssistantInstructions());
  }

  @Override
  protected void populateSpecializedCommitMessageReviewParameters() {
    promptingParameters.put(
        "AssistantCommitMessageInstructions",
        chatGptPrompt.getDefaultGptAssistantInstructions());
  }

  @Override
  protected void populateReviewParameters() {
    promptingParameters.put(
        "AssistantInstructions", chatGptPrompt.getDefaultGptAssistantInstructions());
  }
}
