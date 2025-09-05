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

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OperationNotSupportedException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class CodeContextPolicyBase extends ClientBase implements ICodeContextPolicy {
  public enum CodeContextPolicies {
    NONE,
    ON_DEMAND,
    UPLOAD_ALL
  }

  public CodeContextPolicyBase(Configuration config) {
    super(config);
  }

  public void setupRunAction(ChatGptRun chatGptRun) {
    log.debug("Setup Run Action skipped with the current code context policy");
  }

  public boolean runActionRequired(ChatGptRunResponse runResponse)
      throws OpenAiConnectionFailException {
    log.debug("Run Action Required checking skipped with the current code context policy");
    return false;
  }

  public String generateVectorStore() throws OpenAiConnectionFailException {
    log.debug("Vector Store generating skipped with the current code context policy");
    return null;
  }

  public void removeVectorStore() throws OperationNotSupportedException {
    log.debug("Vector Store removal skipped with the current code context policy");
    throw new OperationNotSupportedException();
  }

  public void updateAssistantTools(
      ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
    log.debug("Assistant Tools updating skipped with the current code context policy");
  }

  public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
    log.debug("Adding Assistant Instructions skipped with the current code context policy");
  }

  public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
    log.debug("Adding Assistant Rules skipped with the current code context policy");
  }
}
