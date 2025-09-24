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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.client.api;

import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.ondemand.CodeContextBuilder;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiGetContextContent;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class LangChainToolExecutor {

  private static final Set<String> ON_DEMAND_FUNCTION_NAMES = Set.of("get_context");
  private static final int MAX_TOOL_EXECUTION_ROUNDS = 1;

  private final Configuration config;
  private final ResponseFormat structuredResponseFormat;
  private final ToolSpecification getContextTool;

  AiMessage execute(ChatModel model, GerritChange change, ChatMemory memory) {
    ChatRequest initialRequest = buildChatRequest(memory.messages());
    ChatResponse response = model.chat(initialRequest);
    AiMessage aiMessage = response != null ? response.aiMessage() : null;

    int iteration = 0;
    while (aiMessage != null
        && aiMessage.hasToolExecutionRequests()
        && iteration < MAX_TOOL_EXECUTION_ROUNDS) {
      iteration++;
      memory.add(aiMessage);
      List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests();
      if (requests == null || requests.isEmpty()) {
        break;
      }
      for (ToolExecutionRequest request : requests) {
        String output = executeToolRequest(request, change);
        memory.add(ToolExecutionResultMessage.from(request, output));
      }
      response = model.chat(buildChatRequest(memory.messages()));
      aiMessage = response != null ? response.aiMessage() : null;
    }

    return aiMessage;
  }

  private ChatRequest buildChatRequest(List<ChatMessage> messages) {
    ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);

    var parametersBuilder = ChatRequestParameters.builder();
    boolean parametersUsed = false;

    if (getContextTool != null) {
      parametersBuilder.toolSpecifications(getContextTool).toolChoice(ToolChoice.AUTO);
      parametersUsed = true;
    }

    if (structuredResponseFormat != null) {
      if (!parametersUsed) {
        requestBuilder.responseFormat(structuredResponseFormat);
      } else {
        parametersBuilder.responseFormat(structuredResponseFormat);
      }
    }

    if (parametersUsed) {
      requestBuilder.parameters(parametersBuilder.build());
    }

    return requestBuilder.build();
  }

  private String executeToolRequest(ToolExecutionRequest request, GerritChange change) {
    if (request == null || getContextTool == null) {
      return "";
    }

    String toolName = request.name();
    if (!ON_DEMAND_FUNCTION_NAMES.contains(toolName)) {
      log.debug("Ignoring unsupported tool request: {}", toolName);
      return "";
    }

    String arguments = request.arguments();
    if (arguments == null || arguments.isBlank()) {
      log.warn("Received empty arguments for tool request: {}", toolName);
      return "";
    }

    try {
      OpenAiGetContextContent getContextContent =
          GsonUtils.jsonToClass(arguments, OpenAiGetContextContent.class);
      if (getContextContent == null) {
        log.warn("Failed to deserialize arguments for tool {}", toolName);
        return "";
      }
      CodeContextBuilder codeContextBuilder =
          new CodeContextBuilder(config, change, new GitRepoFiles());
      return codeContextBuilder.buildCodeContext(getContextContent);
    } catch (Exception e) {
      log.warn("Error executing tool request {}", toolName, e);
      return "";
    }
  }
}
