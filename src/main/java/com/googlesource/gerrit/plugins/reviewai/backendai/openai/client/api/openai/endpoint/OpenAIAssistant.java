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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint;

import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.prompt.AIPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt.OpenAIPromptBase;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIParameters;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAITools;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIApiBase;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAIAssistant extends OpenAIApiBase {
  @Getter private final String description;
  @Getter private final String instructions;
  @Getter private final String model;
  @Getter private final Double temperature;
  private final ICodeContextPolicy codeContextPolicy;

  private OpenAIAssistantTools openAIAssistantTools;

  public OpenAIAssistant(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config);
    log.debug(
        "Setting up assistant parameters based on current configuration and change set data.");
    IOpenAIPrompt openAIPromptOpenAI =
        AIPromptFactory.getOpenAIPromptOpenAI(config, changeSetData, change, codeContextPolicy);
    OpenAIParameters openAIParameters = new OpenAIParameters(config, change.getIsCommentEvent());
    this.codeContextPolicy = codeContextPolicy;
    description = openAIPromptOpenAI.getDefaultGptAssistantDescription();
    instructions = openAIPromptOpenAI.getDefaultGptAssistantInstructions();
    model = config.getGptModel();
    temperature = openAIParameters.getGptTemperature();
  }

  public String createAssistant(String vectorStoreId) throws OpenAiConnectionFailException {
    log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
    Request request = createRequest(vectorStoreId);
    log.debug("OpenAI Create Assistant request: {}", request);

    OpenAIResponse assistantResponse = getOpenAIResponse(request);
    log.debug("Assistant created: {}", assistantResponse);

    return assistantResponse.getId();
  }

  private Request createRequest(String vectorStoreId) {
    log.debug("Creating request to build new assistant.");
    String uri = OpenAIUriResourceLocator.assistantCreateUri();
    log.debug("OpenAI Create Assistant request URI: {}", uri);
    updateTools(vectorStoreId);

    OpenAICreateAssistantRequestBody requestBody =
        OpenAICreateAssistantRequestBody.builder()
            .name(OpenAIPromptBase.DEFAULT_GPT_ASSISTANT_NAME)
            .description(description)
            .instructions(instructions)
            .model(model)
            .temperature(temperature)
            .tools(openAIAssistantTools.getTools())
            .toolResources(openAIAssistantTools.getToolResources())
            .build();
    log.debug("Request body for creating assistant: {}", requestBody);

    return httpClient.createRequestFromJson(uri, requestBody);
  }

  private void updateTools(String vectorStoreId) {
    OpenAITools openAIFormatRepliesTools = new OpenAITools(OpenAITools.Functions.formatReplies);
    openAIAssistantTools =
        OpenAIAssistantTools.builder()
            .tools(new ArrayList<>(List.of(openAIFormatRepliesTools.retrieveFunctionTool())))
            .build();
    codeContextPolicy.updateAssistantTools(openAIAssistantTools, vectorStoreId);
  }
}
