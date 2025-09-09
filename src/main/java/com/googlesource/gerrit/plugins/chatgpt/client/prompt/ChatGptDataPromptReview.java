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
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptDataPromptReview extends ChatGptDataPromptBase implements IChatGptDataPrompt {
  public ChatGptDataPromptReview(
      Configuration config,
      ChangeSetData changeSetData,
      GerritClientData gerritClientData,
      Localizer localizer) {
    super(config, changeSetData, gerritClientData, localizer);
    commentProperties = new ArrayList<>(commentData.getCommentMap().values());
    log.debug("ChatGptDataPromptReview initialized with comment properties.");
  }

  @Override
  public void addMessageItem(int i) {
    log.debug("Adding message item for review at index: {}", i);
    ChatGptMessageItem messageItem = getMessageItem(i);
    if (messageItem.getHistory() != null) {
      messageItems.add(messageItem);
      log.debug("Message item added with history: {}", messageItem);
    } else {
      log.debug("Message item not added due to empty history at index: {}", i);
    }
  }

  @Override
  protected ChatGptMessageItem getMessageItem(int i) {
    log.debug("Retrieving message item for review at index: {}", i);
    ChatGptMessageItem messageItem = super.getMessageItem(i);
    List<ChatGptRequestMessage> messageHistory =
        gptMessageHistory.retrieveHistory(commentProperties.get(i), true);
    setHistory(messageItem, messageHistory);
    log.debug("Message item populated with history for review: {}", messageItem);
    return messageItem;
  }
}
