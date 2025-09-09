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

package com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.client.prompt.ChatGptPromptFactory;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptCreateAssistantRequestBody;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptResponse;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.client.api.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.client.prompt.ChatGptPromptBase;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptAssistant extends ChatGptApiBase {
  @Getter private final String description;
  @Getter private final String instructions;
  @Getter private final String model;
  @Getter private final Double temperature;
  private final ICodeContextPolicy codeContextPolicy;

  private ChatGptAssistantTools chatGptAssistantTools;

  public ChatGptAssistant(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config);
    log.debug(
        "Setting up assistant parameters based on current configuration and change set data.");
    IChatGptPrompt chatGptPrompt =
        ChatGptPromptFactory.getChatGptPrompt(config, changeSetData, change, codeContextPolicy);
    ChatGptParameters chatGptParameters = new ChatGptParameters(config, change.getIsCommentEvent());
    this.codeContextPolicy = codeContextPolicy;
    description = chatGptPrompt.getDefaultGptAssistantDescription();
    instructions = chatGptPrompt.getDefaultGptAssistantInstructions();
    model = config.getGptModel();
    temperature = chatGptParameters.getGptTemperature();
  }

  public String createAssistant(String vectorStoreId) throws OpenAiConnectionFailException {
    log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
    Request request = createRequest(vectorStoreId);
    log.debug("ChatGPT Create Assistant request: {}", request);

    ChatGptResponse assistantResponse = getChatGptResponse(request);
    log.debug("Assistant created: {}", assistantResponse);

    return assistantResponse.getId();
  }

  private Request createRequest(String vectorStoreId) {
    log.debug("Creating request to build new assistant.");
    String uri = UriResourceLocator.assistantCreateUri();
    log.debug("ChatGPT Create Assistant request URI: {}", uri);
    updateTools(vectorStoreId);

    ChatGptCreateAssistantRequestBody requestBody =
        ChatGptCreateAssistantRequestBody.builder()
            .name(ChatGptPromptBase.DEFAULT_GPT_ASSISTANT_NAME)
            .description(description)
            .instructions(instructions)
            .model(model)
            .temperature(temperature)
            .tools(chatGptAssistantTools.getTools())
            .toolResources(chatGptAssistantTools.getToolResources())
            .build();
    log.debug("Request body for creating assistant: {}", requestBody);

    return httpClient.createRequestFromJson(uri, requestBody);
  }

  private void updateTools(String vectorStoreId) {
    ChatGptTools chatGptFormatRepliesTools = new ChatGptTools(ChatGptTools.Functions.formatReplies);
    chatGptAssistantTools =
        ChatGptAssistantTools.builder()
            .tools(new ArrayList<>(List.of(chatGptFormatRepliesTools.retrieveFunctionTool())))
            .build();
    codeContextPolicy.updateAssistantTools(chatGptAssistantTools, vectorStoreId);
  }
}
