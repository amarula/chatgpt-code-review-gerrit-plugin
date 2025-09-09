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
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class ChatGptDataPromptRequests extends ChatGptDataPromptRequestsImpl
    implements IChatGptDataPrompt {
  private static final String REPLY_MESSAGE_REFERENCE = " (Ref. message \"%s\")";

  public ChatGptDataPromptRequests(
      Configuration config,
      ChangeSetData changeSetData,
      GerritClientData gerritClientData,
      Localizer localizer) {
    super(config, changeSetData, gerritClientData, localizer);
    log.debug("ChatGptDataPromptRequests initialized");
  }

  @Override
  protected ChatGptMessageItem getMessageItem(int i) {
    log.debug("Getting Message Item");
    ChatGptMessageItem messageItem = new ChatGptMessageItem();
    setRequestFromCommentProperty(messageItem, i);
    String inReplyToMessage = getReferenceToLastMessage(i);
    if (inReplyToMessage != null) {
      log.debug("In-reply-to message found: {}", inReplyToMessage);
      messageItem.appendToRequest(inReplyToMessage);
    }
    log.debug("Returned Message Item: {}", messageItem);
    return messageItem;
  }

  private String getReferenceToLastMessage(int i) {
    GerritComment comment = commentProperties.get(i);
    log.debug("Getting reference to last message for comment {}", comment);
    String inReplyToId = comment.getInReplyTo();
    if (inReplyToId == null || inReplyToId.isEmpty()) {
      return null;
    }
    HashMap<String, GerritComment> commentMap = gptMessageHistory.getCommentMap();
    log.debug("Getting Comment Map: {}", commentMap);
    if (commentMap.isEmpty()) {
      return null;
    }
    GerritComment inReplyToComment = commentMap.get(inReplyToId);
    if (inReplyToComment == null) {
      return null;
    }
    String referenceToLastMessage =
        String.format(
            REPLY_MESSAGE_REFERENCE, gptMessageHistory.getCleanedMessage(inReplyToComment));
    log.debug("Reference to last message: {}", referenceToLastMessage);
    return referenceToLastMessage;
  }
}
