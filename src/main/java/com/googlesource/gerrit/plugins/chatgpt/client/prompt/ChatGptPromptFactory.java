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

package com.googlesource.gerrit.plugins.chatgpt.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptClient.ReviewAssistantStages;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptPromptFactory {

  public static IChatGptPrompt getChatGptPrompt(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    if (change.getIsCommentEvent()) {
      log.info("ChatGptPromptFactory: Return ChatGptPromptRequests");
      return new ChatGptPromptRequests(config, changeSetData, change, codeContextPolicy);
    } else {
      ChatGptParameters chatGptParameters = new ChatGptParameters(config, false);
      if (chatGptParameters.shouldSpecializeAssistants() || changeSetData.getForcedStagedReview()) {
        return switch (changeSetData.getReviewAssistantStage()) {
          case REVIEW_CODE -> {
            log.info("ChatGptPromptFactory: Return ChatGptPromptReviewCode");
            yield new ChatGptPromptReviewCode(config, changeSetData, change, codeContextPolicy);
          }
          case REVIEW_COMMIT_MESSAGE -> {
            log.info("ChatGptPromptFactory: Return ChatGptPromptReviewCommitMessage");
            yield new ChatGptPromptReviewCommitMessage(
                config, changeSetData, change, codeContextPolicy);
          }
          case REVIEW_REITERATED -> {
            log.info("ChatGptPromptFactory: Return ChatGptPromptReviewReiterate");
            yield new ChatGptPromptReviewReiterated(
                config, changeSetData, change, codeContextPolicy);
          }
        };
      } else {
        log.info("ChatGptPromptFactory: Return ChatGptPromptReview for Unified Review");
        return new ChatGptPromptReview(config, changeSetData, change, codeContextPolicy);
      }
    }
  }

  public static IChatGptPrompt getChatGptPrompt(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      ReviewAssistantStages reviewAssistantStage) {
    changeSetData.setReviewAssistantStage(reviewAssistantStage);
    return getChatGptPrompt(config, changeSetData, change, codeContextPolicy);
  }

  public static IChatGptDataPrompt getChatGptDataPrompt(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      GerritClientData gerritClientData,
      Localizer localizer) {
    if (change.getIsCommentEvent()) {
      log.info("ChatGptPromptFactory: Return ChatGptDataPromptRequests");
      return new ChatGptDataPromptRequests(config, changeSetData, gerritClientData, localizer);
    } else {
      log.info("ChatGptPromptFactory: Return ChatGptDataPromptReview");
      return new ChatGptDataPromptReview(config, changeSetData, gerritClientData, localizer);
    }
  }
}
