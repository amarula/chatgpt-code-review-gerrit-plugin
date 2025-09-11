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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.gerrit.GerritClientPatchSetHelper.filterPatchWithoutCommitMessage;

@Slf4j
public class OpenAIPromptReviewCode extends OpenAIPromptReview implements IOpenAIPrompt {

  public OpenAIPromptReviewCode(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config, changeSetData, change, codeContextPolicy);
    log.debug("OpenAIPromptReviewCode initialized for project: {}", change.getProjectName());
  }

  @Override
  public void addGptAssistantInstructions(List<String> instructions) {
    addReviewInstructions(instructions);
    log.debug("Review Code specific GPT Assistant Instructions added: {}", instructions);
  }

  @Override
  public String getDefaultGptThreadReviewMessage(String patchSet) {
    String filteredPatchSet = filterPatchWithoutCommitMessage(change, patchSet);
    log.debug("Filtered Patch Set for Review Code: {}", filteredPatchSet);
    return super.getDefaultGptThreadReviewMessage(filteredPatchSet);
  }
}
