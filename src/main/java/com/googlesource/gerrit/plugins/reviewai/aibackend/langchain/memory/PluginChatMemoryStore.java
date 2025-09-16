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
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.messages.LangChainMessageTextExtractor;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class PluginChatMemoryStore implements ChatMemoryStore {
  private static final String KEY_MESSAGES_PREFIX = "lc_chat_memory_messages";

  private final PluginDataHandler pluginDataHandler;

  public PluginChatMemoryStore(PluginDataHandler pluginDataHandler) {
    this.pluginDataHandler = pluginDataHandler;
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    String key = keyFor(memoryId);
    try {
      String json = pluginDataHandler.getValue(key);
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
      log.info(
          "Loaded {} chat messages from LangChain memory store for {}", result.size(), memoryId);
      return result;
    } catch (Exception e) {
      log.warn("Failed to get chat memory messages for {}; returning empty list", memoryId, e);
      return new ArrayList<>();
    }
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    String key = keyFor(memoryId);
    try {
      List<StoredMessage> stored = new ArrayList<>();
      if (messages != null) {
        for (ChatMessage m : messages) {
          stored.add(fromChatMessage(m));
        }
      }
      pluginDataHandler.setJsonValue(key, new StoredMessageList(stored));
      log.info(
          "Persisted {} chat messages into LangChain memory store for {}", stored.size(), memoryId);
    } catch (Exception e) {
      log.warn("Failed to persist chat memory messages for {}", memoryId, e);
    }
  }

  @Override
  public void deleteMessages(Object memoryId) {
    log.info("Clearing LangChain memory store for {}", memoryId);
    pluginDataHandler.removeValue(keyFor(memoryId));
  }

  private String keyFor(Object memoryId) {
    return String.format("%s_%s", KEY_MESSAGES_PREFIX, memoryId);
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
      log.warn(
          "Falling back to UserMessage for messageType {} due to error: {}",
          messageType,
          e.getMessage());
      return createUserMessage(text);
    }
  }

  private static StoredMessage fromChatMessage(ChatMessage msg) {
    String text = LangChainMessageTextExtractor.extractText(msg);
    StoredMessage.MessageType messageType = resolveMessageType(msg);
    return new StoredMessage(messageType, text);
  }

  private static StoredMessage.MessageType resolveMessageType(ChatMessage msg) {
    if (msg == null) {
      log.warn(
          "Encountered null chat message while resolving StoredMessage type; defaulting to USER");
      return StoredMessage.MessageType.USER;
    }

    return switch (msg) {
      case SystemMessage ignored -> StoredMessage.MessageType.SYSTEM;
      case AiMessage ignored -> StoredMessage.MessageType.AI;
      case UserMessage ignored -> StoredMessage.MessageType.USER;
      default -> StoredMessage.MessageType.USER;
    };
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
