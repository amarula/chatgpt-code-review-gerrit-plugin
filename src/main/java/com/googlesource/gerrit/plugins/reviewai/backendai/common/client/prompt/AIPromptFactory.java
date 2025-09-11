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

package com.googlesource.gerrit.plugins.reviewai.backendai.common.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.prompt.IAIDataPrompt;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIParameters;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIClient.ReviewAssistantStages;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIPromptFactory {

  public static IOpenAIPrompt getOpenAIPromptOpenAI(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    if (change.getIsCommentEvent()) {
      log.info("AIPromptFactory: Return OpenAIPromptRequests");
      return new OpenAIPromptRequests(config, changeSetData, change, codeContextPolicy);
    } else {
      OpenAIParameters openAIParameters = new OpenAIParameters(config, false);
      if (openAIParameters.shouldSpecializeAssistants() || changeSetData.getForcedStagedReview()) {
        return switch (changeSetData.getReviewAssistantStage()) {
          case REVIEW_CODE -> {
            log.info("AIPromptFactory: Return OpenAIPromptReviewCode");
            yield new OpenAIPromptReviewCode(config, changeSetData, change, codeContextPolicy);
          }
          case REVIEW_COMMIT_MESSAGE -> {
            log.info("AIPromptFactory: Return OpenAIPromptReviewCommitMessage");
            yield new OpenAIPromptReviewCommitMessage(
                config, changeSetData, change, codeContextPolicy);
          }
          case REVIEW_REITERATED -> {
            log.info("AIPromptFactory: Return OpenAIPromptOpenAIReviewReiterate");
            yield new OpenAIPromptReviewReiterated(
                config, changeSetData, change, codeContextPolicy);
          }
        };
      } else {
        log.info("AIPromptFactory: Return OpenAIPromptReview for Unified Review");
        return new OpenAIPromptReview(config, changeSetData, change, codeContextPolicy);
      }
    }
  }

  public static IOpenAIPrompt getOpenAIPromptOpenAI(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      ReviewAssistantStages reviewAssistantStage) {
    changeSetData.setReviewAssistantStage(reviewAssistantStage);
    return getOpenAIPromptOpenAI(config, changeSetData, change, codeContextPolicy);
  }

  public static IAIDataPrompt getOpenAIDataPrompt(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      GerritClientData gerritClientData,
      Localizer localizer) {
    if (change.getIsCommentEvent()) {
      log.info("AIPromptFactory: Return OpenAIDataPromptRequests");
      return new OpenAIDataPromptRequests(config, changeSetData, gerritClientData, localizer);
    } else {
      log.info("AIPromptFactory: Return AIDataPromptReview");
      return new AIDataPromptReview(config, changeSetData, gerritClientData, localizer);
    }
  }
}
