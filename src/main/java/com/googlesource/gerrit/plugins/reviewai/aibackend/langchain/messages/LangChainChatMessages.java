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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.messages;

import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiHistory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.CommentData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiRequestMessage;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LangChainChatMessages {
  private LangChainChatMessages() {}

  public static SystemMessage systemMessage(String text) {
    return createMessage(SystemMessage.class, text);
  }

  public static UserMessage userMessage(String text) {
    return createMessage(UserMessage.class, text);
  }

  public static AiMessage aiMessage(String text) {
    return createMessage(AiMessage.class, text);
  }

  public static List<ChatMessage> build(
      AiHistory aiHistory, GerritClientData gerritClientData, GerritChange change) {
    // Combine patch set history with inline threads.
    List<ChatMessage> historyMessages = new ArrayList<>();

    appendMessages(historyMessages, aiHistory.retrieveHistory(patchSetMarker(), true));

    boolean filterInactive = !change.getIsCommentEvent();
    Set<String> processedCommentIds = new LinkedHashSet<>();

    CommentData commentData = gerritClientData.getCommentData();
    if (commentData != null && commentData.getCommentProperties() != null) {
      for (GerritComment comment : commentData.getCommentProperties()) {
        addCommentThread(historyMessages, aiHistory, comment, filterInactive, processedCommentIds);
      }
    }

    for (GerritComment comment : aiHistory.getCommentMap().values()) {
      addCommentThread(historyMessages, aiHistory, comment, filterInactive, processedCommentIds);
    }

    return historyMessages;
  }

  private static void addCommentThread(
      List<ChatMessage> historyMessages,
      AiHistory aiHistory,
      GerritComment comment,
      boolean filterInactive,
      Set<String> processedCommentIds) {
    if (comment == null) {
      return;
    }
    String id = comment.getId();
    if (id != null && !processedCommentIds.add(id)) {
      return;
    }
    appendMessages(historyMessages, aiHistory.retrieveHistory(comment, filterInactive));
  }

  private static void appendMessages(List<ChatMessage> messages, List<AiRequestMessage> source) {
    // Render request messages into the native LangChain message hierarchy.
    if (source == null) {
      return;
    }
    for (AiRequestMessage message : source) {
      if (message == null) {
        continue;
      }
      String content = message.getContent();
      if (content == null || content.isBlank()) {
        continue;
      }
      messages.add(toChatMessage(message));
    }
  }

  private static ChatMessage toChatMessage(AiRequestMessage message) {
    String role = message.getRole();
    String text = message.getContent();
    if (role == null) {
      return userMessage(text);
    }
    return switch (role.toLowerCase(Locale.ROOT)) {
      case "system" -> systemMessage(text);
      case "assistant" -> aiMessage(text);
      default -> userMessage(text);
    };
  }

  private static GerritComment patchSetMarker() {
    GerritComment patchSetComment = new GerritComment();
    patchSetComment.setFilename(Settings.GERRIT_PATCH_SET_FILENAME);
    return patchSetComment;
  }

  private static <T extends ChatMessage> T createMessage(Class<T> type, String text) {
    try {
      Method from = type.getMethod("from", String.class);
      return type.cast(from.invoke(null, text));
    } catch (Exception ignore) {
    }
    try {
      Constructor<T> constructor = type.getConstructor(String.class);
      return constructor.newInstance(text);
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate message of type " + type.getSimpleName(), e);
    }
  }
}
