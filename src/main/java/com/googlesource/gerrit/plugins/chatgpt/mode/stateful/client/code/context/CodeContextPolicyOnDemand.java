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
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptRunActionHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulReview.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_ON_DEMAND_REQUEST;

@Slf4j
public class CodeContextPolicyOnDemand extends CodeContextPolicyBase implements ICodeContextPolicy {
  private final GerritChange change;
  private final GitRepoFiles gitRepoFiles;

  private ChatGptRunActionHandler chatGptRunActionHandler;

  @VisibleForTesting
  @Inject
  public CodeContextPolicyOnDemand(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config);
    this.change = change;
    this.gitRepoFiles = gitRepoFiles;
    log.debug("CodeContextPolicyOnDemand initialized");
  }

  public void setupRunAction(ChatGptRun chatGptRun) {
    chatGptRunActionHandler = new ChatGptRunActionHandler(config, change, gitRepoFiles, chatGptRun);
    log.debug("Run Action setup with On-Demand code context policy");
  }

  @Override
  public boolean runActionRequired(ChatGptRunResponse runResponse)
      throws OpenAiConnectionFailException {
    log.debug("Checking Run Action Required with On-Demand code context policy");
    return chatGptRunActionHandler.runActionRequired(runResponse);
  }

  @Override
  public void updateAssistantTools(
      ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
    ChatGptTools chatGptGetContextTools = new ChatGptTools(ChatGptTools.Functions.getContext);
    chatGptAssistantTools.getTools().add(chatGptGetContextTools.retrieveFunctionTool());
    log.debug(
        "Updated Assistant Tools for On-Demand code context policy: {}", chatGptAssistantTools);
  }

  @Override
  public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
    rules.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_ON_DEMAND_REQUEST);
    log.debug("Added Assistant Rules for On-Demand code context policy");
  }
}
