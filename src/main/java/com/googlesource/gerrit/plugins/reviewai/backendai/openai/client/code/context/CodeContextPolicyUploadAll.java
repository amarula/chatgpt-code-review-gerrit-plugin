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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAITool;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIVectorStoreHandler;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIAssistantTools;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIToolResources;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt.OpenAIPromptBase.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
import static com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt.OpenAIPromptReview.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;

@Slf4j
public class CodeContextPolicyUploadAll extends CodeContextPolicyBase
    implements ICodeContextPolicy {
  private final GerritChange change;
  private final OpenAIVectorStoreHandler openAIVectorStoreHandler;

  @Getter private OpenAIToolResources toolResources;

  @VisibleForTesting
  @Inject
  public CodeContextPolicyUploadAll(
      Configuration config,
      GerritChange change,
      GitRepoFiles gitRepoFiles,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.change = change;
    openAIVectorStoreHandler =
        new OpenAIVectorStoreHandler(
            config, change, gitRepoFiles, pluginDataHandlerProvider.getProjectScope());
    log.debug("CodeContextPolicyUploadAll initialized");
  }

  @Override
  public String generateVectorStore() throws OpenAiConnectionFailException {
    log.debug("Generating Vector Store");
    return openAIVectorStoreHandler.generateVectorStore();
  }

  @Override
  public void removeVectorStore() {
    openAIVectorStoreHandler.removeVectorStoreId();
    log.debug("Vector Store removed");
  }

  @Override
  public void updateAssistantTools(
      OpenAIAssistantTools openAIAssistantTools, String vectorStoreId) {
    openAIAssistantTools.getTools().add(new OpenAITool("file_search"));
    openAIAssistantTools.setToolResources(
        new OpenAIToolResources(
            new OpenAIToolResources.VectorStoreIds(new String[] {vectorStoreId})));
    log.debug("Updated Assistant Tools for Upload-All code context policy");
  }

  @Override
  public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
    instructions.add(
        String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT, change.getProjectName()));
    log.debug("Added Assistant Instructions for Upload-All code context policy");
  }

  @Override
  public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
    rules.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE);
    log.debug("Added Assistant Rules for Upload-All code context policy");
  }
}
