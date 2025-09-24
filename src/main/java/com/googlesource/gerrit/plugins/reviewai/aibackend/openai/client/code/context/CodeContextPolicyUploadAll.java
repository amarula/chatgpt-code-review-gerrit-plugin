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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.code.context.CodeContextPolicyBase;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiTool;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiVectorStoreHandler;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiAssistantTools;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiToolResources;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.prompt.AiPromptBase.DEFAULT_AI_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
import static com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.prompt.AiPromptReview.DEFAULT_AI_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;

@Slf4j
public class CodeContextPolicyUploadAll extends CodeContextPolicyBase
    implements ICodeContextPolicy {
  private final GerritChange change;
  private final OpenAiVectorStoreHandler openAiVectorStoreHandler;

  @Getter private OpenAiToolResources toolResources;

  @VisibleForTesting
  @Inject
  public CodeContextPolicyUploadAll(
      Configuration config,
      GerritChange change,
      GitRepoFiles gitRepoFiles,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.change = change;
    openAiVectorStoreHandler =
        new OpenAiVectorStoreHandler(
            config, change, gitRepoFiles, pluginDataHandlerProvider.getProjectScope());
    log.debug("CodeContextPolicyUploadAll initialized");
  }

  @Override
  public String generateVectorStore() throws AiConnectionFailException {
    log.debug("Generating Vector Store");
    return openAiVectorStoreHandler.generateVectorStore();
  }

  @Override
  public void removeVectorStore() {
    openAiVectorStoreHandler.removeVectorStoreId();
    log.debug("Vector Store removed");
  }

  @Override
  public void updateAssistantTools(
      OpenAiAssistantTools openAiAssistantTools, String vectorStoreId) {
    openAiAssistantTools.getTools().add(new OpenAiTool("file_search"));
    openAiAssistantTools.setToolResources(
        new OpenAiToolResources(
            new OpenAiToolResources.VectorStoreIds(new String[] {vectorStoreId})));
    log.debug("Updated Assistant Tools for Upload-All code context policy");
  }

  @Override
  public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
    instructions.add(
        String.format(DEFAULT_AI_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT, change.getProjectName()));
    log.debug("Added Assistant Instructions for Upload-All code context policy");
  }

  @Override
  public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
    rules.add(DEFAULT_AI_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE);
    log.debug("Added Assistant Rules for Upload-All code context policy");
  }
}
