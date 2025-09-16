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
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.memory.PluginChatMemoryStore;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiResponseContent;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.api.ai.IAiClient;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.isJsonObjectAsString;
import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.unwrapJsonCode;

@Slf4j
@Singleton
public class LangChainClient extends OpenAiClientBase implements IAiClient {

  private static final String KEY_INSTRUCTIONS_ADDED = "lc_instructions_added";

  private final ICodeContextPolicy codeContextPolicy;
  private final PluginDataHandlerProvider pluginDataHandlerProvider;
  private final LangChainTokenEstimatorProvider tokenEstimatorProvider;

  private String requestBody;

  @Inject
  public LangChainClient(
      Configuration config,
      ICodeContextPolicy codeContextPolicy,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.codeContextPolicy = codeContextPolicy;
    this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    this.tokenEstimatorProvider = new LangChainTokenEstimatorProvider(config);
    log.debug("Initialized LangChainClient");
  }

  @Override
  public OpenAiResponseContent ask(
      ChangeSetData changeSetData, GerritChange change, String patchSet) throws Exception {
    try {
      // Build prompts
      var prompt = AiPromptFactory.getAiPrompt(config, changeSetData, change, codeContextPolicy);
      String systemInstructions = prompt.getDefaultAiAssistantInstructions();
      String userMessage = prompt.getDefaultAiThreadReviewMessage(patchSet);
      Object memoryId = change.getFullChangeId();

      log.info("LangChain system instructions for {}: {}", memoryId, systemInstructions);
      log.info("LangChain user prompt for {} patchSet {}: {}", memoryId, patchSet, userMessage);

      // Prepare memory, persisted per change scope
      var changeScope = pluginDataHandlerProvider.getChangeScope();
      var memoryStore = new PluginChatMemoryStore(changeScope);
      log.info("LangChain initializing chat memory for {}", memoryId);
      ChatMemory memory =
          TokenWindowChatMemory.builder()
              .id(memoryId)
              .maxTokens(config.getLcMaxMemoryTokens(), tokenEstimatorProvider.get())
              .chatMemoryStore(memoryStore)
              .build();
      log.info("LangChain chat memory ready for {}", memoryId);

      // Add system instructions once per thread
      String instructionsAdded = changeScope.getValue(KEY_INSTRUCTIONS_ADDED);
      if (instructionsAdded == null || instructionsAdded.isEmpty()) {
        try {
          memory.add(SystemMessage.from(systemInstructions));
        } catch (Throwable t) {
          // Fallback for older method signatures
          memory.add(new SystemMessage(systemInstructions));
        }
        log.info("LangChain system instructions added to memory for {}", memoryId);
        changeScope.setValue(KEY_INSTRUCTIONS_ADDED, "true");
        log.info("LangChain system instructions persisted for {}", memoryId);
      }

      // Add user message
      try {
        log.info("LangChain adding user message to memory for {}", memoryId);
        memory.add(UserMessage.from(userMessage));
      } catch (Throwable t) {
        memory.add(new UserMessage(userMessage));
      }
      log.info("LangChain user message stored in memory for {}", memoryId);
      requestBody = userMessage; // exposed for tests/inspection

      // Build model from config
      double temperature =
          change.getIsCommentEvent()
              ? Double.parseDouble(config.getAiCommentTemperature())
              : Double.parseDouble(config.getAiReviewTemperature());

      String baseUrl = config.getAiDomain();
      // Some providers expect `/v1`; only append for default OpenAI
      if (Configuration.OPENAI_DOMAIN.equals(baseUrl)) {
        baseUrl = baseUrl.endsWith("/v1") ? baseUrl : baseUrl + "/v1";
      }

      ChatModel model =
          OpenAiChatModel.builder()
              .baseUrl(baseUrl)
              .apiKey(config.getAiToken())
              .modelName(config.getAiModel())
              .temperature(temperature)
              .timeout(Duration.ofSeconds(config.getAiConnectionTimeout()))
              .build();

      // Generate response using simple string interface for compatibility
      log.info(
          "LangChain request for {} using model {} (temperature={}, baseUrl={})",
          memoryId,
          config.getAiModel(),
          temperature,
          baseUrl);

      String responseText = model.chat(userMessage);

      // Persist AI message into memory for continuity
      AiMessage ai;
      try {
        ai = AiMessage.from(responseText);
      } catch (Throwable t) {
        ai = new AiMessage(responseText);
      }
      memory.add(ai);

      if (responseText == null) {
        log.warn("LangChain model returned null response text");
        return null;
      }

      if (isJsonObjectAsString(responseText)) {
        return convertResponseContentFromJson(unwrapJsonCode(responseText));
      }
      return new OpenAiResponseContent(responseText);
    } catch (Exception e) {
      log.warn("Error while processing LangChain request", e);
      throw new OpenAiConnectionFailException(e);
    }
  }

  @Override
  public String getRequestBody() {
    return requestBody;
  }
}
