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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStoreHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptToolResources;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulBase.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulReview.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;

@Slf4j
public class CodeContextPolicyUploadAll extends CodeContextPolicyBase
    implements ICodeContextPolicy {
  private final GerritChange change;
  private final ChatGptVectorStoreHandler chatGptVectorStoreHandler;

  @Getter private ChatGptToolResources toolResources;

  @VisibleForTesting
  @Inject
  public CodeContextPolicyUploadAll(
      Configuration config,
      GerritChange change,
      GitRepoFiles gitRepoFiles,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.change = change;
    chatGptVectorStoreHandler =
        new ChatGptVectorStoreHandler(
            config, change, gitRepoFiles, pluginDataHandlerProvider.getProjectScope());
    log.debug("CodeContextPolicyUploadAll initialized");
  }

  @Override
  public String generateVectorStore() throws OpenAiConnectionFailException {
    log.debug("Generating Vector Store");
    return chatGptVectorStoreHandler.generateVectorStore();
  }

  @Override
  public void removeVectorStore() {
    chatGptVectorStoreHandler.removeVectorStoreId();
    log.debug("Vector Store removed");
  }

  @Override
  public void updateAssistantTools(
      ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
    chatGptAssistantTools.getTools().add(new ChatGptTool("file_search"));
    chatGptAssistantTools.setToolResources(
        new ChatGptToolResources(
            new ChatGptToolResources.VectorStoreIds(new String[] {vectorStoreId})));
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
