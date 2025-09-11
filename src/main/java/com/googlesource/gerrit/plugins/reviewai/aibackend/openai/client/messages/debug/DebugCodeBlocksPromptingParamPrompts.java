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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.messages.debug;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;

public class DebugCodeBlocksPromptingParamPrompts extends DebugCodeBlocksPromptingParamBase {
  private static final String PATCH_SET_PLACEHOLDER = "<PATCH_SET>";
  private static final String COMMIT_MESSAGE_PATCH_TEMPLATE =
      "Subject: <COMMIT_MESSAGE> Change-Id: ... " + PATCH_SET_PLACEHOLDER;

  private final Configuration config;
  private final ChangeSetData changeSetData;

  public DebugCodeBlocksPromptingParamPrompts(
      Localizer localizer,
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(
        localizer, "message.dump.prompts.title", config, changeSetData, change, codeContextPolicy);
    this.config = config;
    this.changeSetData = changeSetData;
  }

  @Override
  protected void populateOpenAISpecializedCodeReviewParameters() {
    promptingParameters.put(
        "ReviewCodePrompt", aIPrompt.getDefaultGptThreadReviewMessage(PATCH_SET_PLACEHOLDER));
  }

  @Override
  protected void populateOpenAISpecializedCommitMessageReviewParameters() {
    promptingParameters.put(
        "ReviewCommitMessagePrompt",
        aIPrompt.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
  }

  @Override
  protected void populateOpenAIReviewParameters() {
    promptingParameters.put(
        "ReviewPrompt",
        aIPrompt.getDefaultGptThreadReviewMessage(COMMIT_MESSAGE_PATCH_TEMPLATE));
  }
}
