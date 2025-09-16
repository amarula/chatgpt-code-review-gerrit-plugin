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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.memory;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.model.StoredMessage;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.model.StoredMessageList;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class PluginChatMemoryStore {
  private static final String KEY_MESSAGES = "lc_chat_memory_messages";

  private final PluginDataHandler pluginDataHandler;

  public PluginChatMemoryStore(PluginDataHandler pluginDataHandler) {
    this.pluginDataHandler = pluginDataHandler;
  }

  public List<ChatMessage> getMessages() {
    try {
      String json = pluginDataHandler.getValue(KEY_MESSAGES);
      if (json == null || json.isEmpty()) {
        return new ArrayList<>();
      }
      StoredMessageList stored = getGson().fromJson(json, StoredMessageList.class);
      if (stored == null || stored.getMessages() == null) {
        return new ArrayList<>();
      }
      List<ChatMessage> result = new ArrayList<>();
      for (StoredMessage m : stored.getMessages()) {
        result.add(toChatMessage(m));
      }
      log.info("Loaded {} chat messages from LangChain memory store", result.size());
      return result;
    } catch (Exception e) {
      log.warn("Failed to get chat memory messages; returning empty list", e);
      return new ArrayList<>();
    }
  }

  public void updateMessages(List<ChatMessage> messages) {
    try {
      List<StoredMessage> stored = new ArrayList<>();
      if (messages != null) {
        for (ChatMessage m : messages) {
          stored.add(fromChatMessage(m));
        }
      }
      pluginDataHandler.setJsonValue(KEY_MESSAGES, new StoredMessageList(stored));
      log.info("Persisted {} chat messages into LangChain memory store", stored.size());
    } catch (Exception e) {
      log.warn("Failed to persist chat memory messages", e);
    }
  }

  public void deleteMessages() {
    log.info("Clearing LangChain memory store");
    pluginDataHandler.removeValue(KEY_MESSAGES);
  }

  private static ChatMessage toChatMessage(StoredMessage sm) {
    StoredMessage.MessageType messageType = sm.getMessageType();
    String text = sm.getText();
    if (messageType == null) {
      log.warn("Stored message messageType missing; defaulting to USER for text preview: {}", text);
      messageType = StoredMessage.MessageType.USER;
    }
    try {
      return switch (messageType) {
        case SYSTEM -> createSystemMessage(text);
        case USER -> createUserMessage(text);
        case AI -> createAiMessage(text);
      };
    } catch (Exception e) {
      log.warn("Falling back to UserMessage for messageType {} due to error: {}", messageType, e.getMessage());
      return createUserMessage(text);
    }
  }

  private static StoredMessage fromChatMessage(ChatMessage msg) {
    String text = safeExtractText(msg);
    StoredMessage.MessageType messageType = resolveMessageType(msg);
    return new StoredMessage(messageType, text);
  }

  private static StoredMessage.MessageType resolveMessageType(ChatMessage msg) {
    if (msg == null) {
      log.warn("Encountered null chat message while resolving StoredMessage type; defaulting to USER");
      return StoredMessage.MessageType.USER;
    }

    return switch (msg) {
      case SystemMessage ignored -> StoredMessage.MessageType.SYSTEM;
      case AiMessage ignored -> StoredMessage.MessageType.AI;
      case UserMessage ignored -> StoredMessage.MessageType.USER;
      default -> StoredMessage.MessageType.USER;
    };
  }

  private static String safeExtractText(ChatMessage msg) {
    try {
      // Try common getters in order
      for (String method : new String[] {"text", "content", "getText"}) {
        try {
          Method m = msg.getClass().getMethod(method);
          if (m.getReturnType().equals(String.class) && Modifier.isPublic(m.getModifiers())) {
            Object value = m.invoke(msg);
            if (value != null) return (String) value;
          }
        } catch (NoSuchMethodException ignored) {
        }
      }
    } catch (Exception e) {
      log.debug("Reflection failed to extract message text: {}", e.getMessage());
    }
    String s = msg.toString();
    return s == null ? "" : s;
  }

  private static SystemMessage createSystemMessage(String text) {
    try {
      // Prefer factory if available
      Method from = SystemMessage.class.getMethod("from", String.class);
      return (SystemMessage) from.invoke(null, text);
    } catch (Exception ignore) {
    }
    return new SystemMessage(text);
  }

  private static UserMessage createUserMessage(String text) {
    try {
      Method from = UserMessage.class.getMethod("from", String.class);
      return (UserMessage) from.invoke(null, text);
    } catch (Exception ignore) {
    }
    return new UserMessage(text);
  }

  private static AiMessage createAiMessage(String text) {
    try {
      Method from = AiMessage.class.getMethod("from", String.class);
      return (AiMessage) from.invoke(null, text);
    } catch (Exception ignore) {
    }
    return new AiMessage(text);
  }
}
