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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint;

import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.prompt.AiPromptBase;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.prompt.IAiPrompt;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiParameters;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiTools;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAiAssistant extends OpenAiApiBase {
  @Getter private final String description;
  @Getter private final String instructions;
  @Getter private final String model;
  @Getter private final Double temperature;
  private final ICodeContextPolicy codeContextPolicy;

  private OpenAiAssistantTools openAiAssistantTools;

  public OpenAiAssistant(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config);
    log.debug(
        "Setting up assistant parameters based on current configuration and change set data.");
    IAiPrompt aiPromptOpenAi =
        AiPromptFactory.getAiPrompt(config, changeSetData, change, codeContextPolicy);
    OpenAiParameters openAiParameters = new OpenAiParameters(config, change.getIsCommentEvent());
    this.codeContextPolicy = codeContextPolicy;
    description = aiPromptOpenAi.getDefaultAiAssistantDescription();
    instructions = aiPromptOpenAi.getDefaultAiAssistantInstructions();
    model = config.aiModel();
    temperature = openAiParameters.getAiTemperature();
  }

  public String createAssistant(String vectorStoreId) throws AiConnectionFailException {
    log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
    Request request = createRequest(vectorStoreId);
    log.debug("OpenAI Create Assistant request: {}", request);

    OpenAiResponse assistantResponse = getOpenAiResponse(request);
    log.debug("Assistant created: {}", assistantResponse);

    return assistantResponse.getId();
  }

  private Request createRequest(String vectorStoreId) {
    log.debug("Creating request to build new assistant.");
    String uri = OpenAiUriResourceLocator.assistantCreateUri();
    log.debug("OpenAI Create Assistant request URI: {}", uri);
    updateTools(vectorStoreId);

    OpenAiCreateAssistantRequestBody requestBody =
        OpenAiCreateAssistantRequestBody.builder()
            .name(AiPromptBase.DEFAULT_AI_ASSISTANT_NAME)
            .description(description)
            .instructions(instructions)
            .model(model)
            .temperature(temperature)
            .tools(openAiAssistantTools.getTools())
            .toolResources(openAiAssistantTools.getToolResources())
            .build();
    log.debug("Request body for creating assistant: {}", requestBody);

    return httpClient.createRequestFromJson(uri, requestBody);
  }

  private void updateTools(String vectorStoreId) {
    OpenAiTools openAiFormatRepliesTools = new OpenAiTools(OpenAiTools.Functions.formatReplies);
    openAiAssistantTools =
        OpenAiAssistantTools.builder()
            .tools(new ArrayList<>(List.of(openAiFormatRepliesTools.retrieveFunctionTool())))
            .build();
    codeContextPolicy.updateAssistantTools(openAiAssistantTools, vectorStoreId);
  }
}
