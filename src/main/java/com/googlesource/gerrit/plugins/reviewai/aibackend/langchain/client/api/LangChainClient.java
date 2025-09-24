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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.ai.AiClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiHistory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiResponseContent;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.messages.LangChainChatMessages;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.model.LangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.LangChainProviderFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.ondemand.CodeContextBuilder;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiGetContextContent;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.api.ai.IAiClient;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.langchain.provider.ILangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.CodeContextPolicyBase.CodeContextPolicies;
import com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ToolChoice;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.isJsonObjectAsString;
import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.unwrapJsonCode;

@Slf4j
@Singleton
public class LangChainClient extends AiClientBase implements IAiClient {

  private static final String FORMAT_REPLIES_SCHEMA_RESOURCE = "config/formatRepliesTool.json";
  private static final String GET_CONTEXT_TOOL_RESOURCE = "config/getContextTool.json";
  private static final Set<String> ON_DEMAND_FUNCTION_NAMES = Set.of("get_context");
  private static final int MAX_TOOL_EXECUTION_ROUNDS = 1;

  private final ICodeContextPolicy codeContextPolicy;
  private final LangChainTokenEstimatorProvider tokenEstimatorProvider;
  private final GerritClient gerritClient;
  private final Localizer localizer;
  private final ResponseFormat structuredResponseFormat;
  private final ToolSpecification getContextTool;

  private String requestBody;

  @Inject
  public LangChainClient(
      Configuration config,
      ICodeContextPolicy codeContextPolicy,
      GerritClient gerritClient,
      Localizer localizer) {
    super(config);
    this.codeContextPolicy = codeContextPolicy;
    this.tokenEstimatorProvider = new LangChainTokenEstimatorProvider(config);
    this.gerritClient = gerritClient;
    this.localizer = localizer;
    this.structuredResponseFormat =
        new LangChainStructuredResponseFactory(FORMAT_REPLIES_SCHEMA_RESOURCE)
            .loadStructuredResponseFormat();
    if (config != null && config.getCodeContextPolicy() == CodeContextPolicies.ON_DEMAND) {
      this.getContextTool =
          new LangChainToolSpecificationFactory(GET_CONTEXT_TOOL_RESOURCE).loadToolSpecification();
    } else {
      this.getContextTool = null;
    }
    log.debug("Initialized LangChainClient");
  }

  @Override
  public AiResponseContent ask(ChangeSetData changeSetData, GerritChange change, String patchSet)
      throws Exception {
    try {
      var prompt = AiPromptFactory.getAiPrompt(config, changeSetData, change, codeContextPolicy);
      String systemInstructions = prompt.getDefaultAiAssistantInstructions();
      String userMessage = prompt.getDefaultAiThreadReviewMessage(patchSet);
      Object memoryId = change.getFullChangeId();

      log.info("LangChain system instructions for {}: {}", memoryId, systemInstructions);
      log.info("LangChain user prompt for {} patchSet {}: {}", memoryId, patchSet, userMessage);

      ChatMemory memory =
          TokenWindowChatMemory.builder()
              .id(memoryId)
              .maxTokens(config.getLcMaxMemoryTokens(), tokenEstimatorProvider.get())
              .build();

      memory.add(LangChainChatMessages.systemMessage(systemInstructions));

      GerritClientData gerritClientData = gerritClient.getClientData(change);
      AiHistory aiHistory = new AiHistory(config, changeSetData, gerritClientData, localizer);
      List<ChatMessage> history = LangChainChatMessages.build(aiHistory, gerritClientData, change);
      for (ChatMessage message : history) {
        memory.add(message);
      }

      memory.add(LangChainChatMessages.userMessage(userMessage));
      requestBody = userMessage; // exposed for tests/inspection

      double temperature =
          change.getIsCommentEvent()
              ? Double.parseDouble(config.getAiCommentTemperature())
              : Double.parseDouble(config.getAiReviewTemperature());

      LangChainProviders providerType = config.getLcProvider();
      ILangChainProvider provider = LangChainProviderFactory.get(providerType);
      LangChainProvider providerModel = provider.buildChatModel(config, temperature);
      ChatModel model = providerModel.getModel();

      log.info(
          "LangChain request for {} using provider {} model {} (temperature={}, endpoint={})",
          memoryId,
          providerType,
          config.getAiModel(),
          temperature,
          providerModel.getEndpoint());

      List<ChatMessage> memorySnapshot = memory.messages();
      log.debug(
          "LangChain memory prepared for {} with {} messages: {}",
          memoryId,
          memorySnapshot.size(),
          memorySnapshot);

      AiMessage ai = executeWithTools(model, change, memory);
      String responseText = ai != null ? ai.text() : null;

      if (responseText == null) {
        log.warn("LangChain model returned null response text");
        return null;
      }

      if (isJsonObjectAsString(responseText)) {
        return convertResponseContentFromJson(unwrapJsonCode(responseText));
      }
      return new AiResponseContent(responseText);
    } catch (Exception e) {
      log.warn("Error while processing LangChain request", e);
      throw new AiConnectionFailException(e);
    }
  }

  @Override
  public String getRequestBody() {
    return requestBody;
  }

  private AiMessage executeWithTools(ChatModel model, GerritChange change, ChatMemory memory) {
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
      parametersBuilder
          .toolSpecifications(getContextTool)
          .toolChoice(ToolChoice.AUTO);
      parametersUsed = true;
    }

    if (structuredResponseFormat != null) {
      if (!parametersUsed) {
        requestBuilder.responseFormat(structuredResponseFormat);
      } else {
        parametersBuilder.responseFormat(structuredResponseFormat);
        parametersUsed = true;
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
