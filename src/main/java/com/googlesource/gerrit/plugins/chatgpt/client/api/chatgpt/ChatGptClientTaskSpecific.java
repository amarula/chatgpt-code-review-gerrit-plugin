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

package com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ChatGptClientTaskSpecific extends ChatGptClient
    implements IChatGptClient {
  private static final List<ReviewAssistantStages> TASK_SPECIFIC_ASSISTANT_STAGES =
      List.of(ReviewAssistantStages.REVIEW_CODE, ReviewAssistantStages.REVIEW_COMMIT_MESSAGE);

  @VisibleForTesting
  @Inject
  public ChatGptClientTaskSpecific(
      Configuration config,
      ICodeContextPolicy codeContextPolicy,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config, codeContextPolicy, pluginDataHandlerProvider);
    log.debug("Initialized ChatGptClientTaskSpecific.");
  }

  public ChatGptResponseContent ask(
      ChangeSetData changeSetData, GerritChange change, String patchSet)
      throws OpenAiConnectionFailException {
    log.debug(
        "Task-specific ChatGPT ask method called with changeId: {}", change.getFullChangeId());
    if (change.getIsCommentEvent()) {
      return super.ask(changeSetData, change, patchSet);
    }
    List<ChatGptResponseContent> chatGptResponseContents = new ArrayList<>();
    for (ReviewAssistantStages assistantStage : TASK_SPECIFIC_ASSISTANT_STAGES) {
      changeSetData.setReviewAssistantStage(assistantStage);
      log.debug("Processing stage: {}", assistantStage);
      chatGptResponseContents.add(super.ask(changeSetData, change, patchSet));
    }
    return mergeResponses(chatGptResponseContents);
  }

  private ChatGptResponseContent mergeResponses(
      List<ChatGptResponseContent> chatGptResponseContents) {
    log.debug("Merging responses from different task-specific stages.");
    ChatGptResponseContent mergedResponse = chatGptResponseContents.remove(0);
    for (ChatGptResponseContent chatGptResponseContent : chatGptResponseContents) {
      List<ChatGptReplyItem> replies = chatGptResponseContent.getReplies();
      if (replies != null) {
        mergedResponse.getReplies().addAll(replies);
      } else {
        mergedResponse.setMessageContent(chatGptResponseContent.getMessageContent());
      }
    }
    log.debug("Merged response content: {}", mergedResponse.getMessageContent());
    return mergedResponse;
  }
}
