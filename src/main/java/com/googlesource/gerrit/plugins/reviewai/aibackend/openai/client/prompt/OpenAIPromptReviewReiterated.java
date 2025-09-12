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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.TextUtils.joinWithNewLine;

@Slf4j
public class OpenAIPromptReviewReiterated extends OpenAIPromptReview implements IOpenAIPrompt {

  public OpenAIPromptReviewReiterated(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config, changeSetData, change, codeContextPolicy);
    log.debug("OpenAIPromptReviewReiterated initialized for project: {}", change.getProjectName());
  }

  @Override
  public String getDefaultGptThreadReviewMessage(String patchSet) {
    return joinWithNewLine(
        List.of(
            DEFAULT_GPT_MESSAGE_REQUEST_RESEND_FORMATTED,
            DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
            DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES));
  }
}
