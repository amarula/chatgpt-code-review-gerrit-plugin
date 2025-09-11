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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.api.ai.IAIClient;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIReplyItem;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIResponseContent;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class OpenAIClientTaskSpecific extends OpenAIClient implements IAIClient {
  private static final List<ReviewAssistantStages> TASK_SPECIFIC_ASSISTANT_STAGES =
      List.of(ReviewAssistantStages.REVIEW_CODE, ReviewAssistantStages.REVIEW_COMMIT_MESSAGE);

  @VisibleForTesting
  @Inject
  public OpenAIClientTaskSpecific(
      Configuration config,
      ICodeContextPolicy codeContextPolicy,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config, codeContextPolicy, pluginDataHandlerProvider);
    log.debug("Initialized OpenAIClientTaskSpecific.");
  }

  public OpenAIResponseContent ask(
      ChangeSetData changeSetData, GerritChange change, String patchSet)
      throws OpenAiConnectionFailException {
    log.debug("Task-specific OpenAI ask method called with changeId: {}", change.getFullChangeId());
    if (change.getIsCommentEvent()) {
      return super.ask(changeSetData, change, patchSet);
    }
    List<OpenAIResponseContent> openAIResponseContents = new ArrayList<>();
    for (ReviewAssistantStages assistantStage : TASK_SPECIFIC_ASSISTANT_STAGES) {
      changeSetData.setReviewAssistantStage(assistantStage);
      log.debug("Processing stage: {}", assistantStage);
      openAIResponseContents.add(super.ask(changeSetData, change, patchSet));
    }
    return mergeResponses(openAIResponseContents);
  }

  private OpenAIResponseContent mergeResponses(List<OpenAIResponseContent> openAIResponseContents) {
    log.debug("Merging responses from different task-specific stages.");
    OpenAIResponseContent mergedResponse = openAIResponseContents.remove(0);
    for (OpenAIResponseContent openAIResponseContent : openAIResponseContents) {
      List<OpenAIReplyItem> replies = openAIResponseContent.getReplies();
      if (replies != null) {
        mergedResponse.getReplies().addAll(replies);
      } else {
        mergedResponse.setMessageContent(openAIResponseContent.getMessageContent());
      }
    }
    log.debug("Merged response content: {}", mergedResponse.getMessageContent());
    return mergedResponse;
  }
}
