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
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIMessageItem;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIRequestMessage;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.OPENAI_ROLE_USER;

@Slf4j
public class AIDataPromptRequests extends AIDataPromptBase {
  protected OpenAIMessageItem messageItem;
  protected List<OpenAIRequestMessage> messageHistory;

  public AIDataPromptRequests(
      Configuration config,
      ChangeSetData changeSetData,
      GerritClientData gerritClientData,
      Localizer localizer) {
    super(config, changeSetData, gerritClientData, localizer);
    commentProperties = commentData.getCommentProperties();
    log.debug("AIDataPromptRequests initialized with comment properties.");
  }

  public void addMessageItem(int i) {
    log.debug("Adding message item for comment index: {}", i);
    OpenAIMessageItem messageItem = getMessageItem(i);
    messageItem.setId(i);
    messageItems.add(messageItem);
    log.debug("Added message item: {}", messageItem);
  }

  @Override
  protected OpenAIMessageItem getMessageItem(int i) {
    messageItem = super.getMessageItem(i);
    log.debug("Retrieving extended message item for index: {}", i);
    messageHistory = gptMessageHistory.retrieveHistory(commentProperties.get(i));
    OpenAIRequestMessage request = extractLastUserMessageFromHistory();
    if (request != null) {
      messageItem.setRequest(request.getContent());
    } else {
      setRequestFromCommentProperty(messageItem, i);
    }
    log.debug("Message item after setting request content: {}", messageItem);
    return messageItem;
  }

  protected void setRequestFromCommentProperty(OpenAIMessageItem messageItem, int i) {
    GerritComment gerritComment = commentProperties.get(i);
    String cleanedMessage = gptMessageHistory.getCleanedMessage(gerritComment);
    log.debug("Getting cleaned Message: {}", cleanedMessage);
    messageItem.setRequest(cleanedMessage);
  }

  private OpenAIRequestMessage extractLastUserMessageFromHistory() {
    log.debug("Extracting last user message from history.");
    for (int i = messageHistory.size() - 1; i >= 0; i--) {
      if (OPENAI_ROLE_USER.equals(messageHistory.get(i).getRole())) {
        OpenAIRequestMessage request = messageHistory.remove(i);
        log.debug("Last user message extracted: {}", request);
        return request;
      }
    }
    log.warn(
        "Error extracting request from message history: no user message found in {}",
        messageHistory);
    return null;
  }
}
