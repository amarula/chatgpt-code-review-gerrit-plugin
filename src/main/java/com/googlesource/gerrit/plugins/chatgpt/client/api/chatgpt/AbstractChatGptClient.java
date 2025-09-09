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

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;

import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractChatGptClient extends ChatGptClientBase {
  protected boolean isCommentEvent = false;
  @Getter protected String requestBody;

  public AbstractChatGptClient(Configuration config) {
    super(config);
    log.debug("ChatGptClient initialized with configuration.");
  }

  protected ChatGptResponseContent getResponseContent(List<ChatGptToolCall> toolCalls) {
    log.debug("Getting response content from tool calls: {}", toolCalls);
    if (toolCalls.size() > 1) {
      return mergeToolCalls(toolCalls);
    } else {
      return getArgumentAsResponse(toolCalls, 0);
    }
  }

  private ChatGptResponseContent mergeToolCalls(List<ChatGptToolCall> toolCalls) {
    log.debug("Merging responses from multiple tool calls.");
    ChatGptResponseContent responseContent = getArgumentAsResponse(toolCalls, 0);
    for (int ind = 1; ind < toolCalls.size(); ind++) {
      responseContent.getReplies().addAll(getArgumentAsResponse(toolCalls, ind).getReplies());
    }
    return responseContent;
  }
}
