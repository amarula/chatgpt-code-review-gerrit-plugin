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

package com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ChatGptRunActionHandler extends ClientBase {
  private static final int MAX_ACTION_REQUIRED_RETRIES = 1;

  private final GerritChange change;
  private final GitRepoFiles gitRepoFiles;
  private final ChatGptRun chatGptRun;

  private int actionRequiredRetries;

  public ChatGptRunActionHandler(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles, ChatGptRun chatGptRun) {
    super(config);
    this.change = change;
    this.gitRepoFiles = gitRepoFiles;
    this.chatGptRun = chatGptRun;
    actionRequiredRetries = 0;
    log.debug("ChatGptRunActionHandler initialized");
  }

  public boolean runActionRequired(ChatGptRunResponse runResponse)
      throws OpenAiConnectionFailException {
    log.debug("Response status: {}", runResponse.getStatus());
    if (ChatGptPoller.isActionRequired(runResponse.getStatus())) {
      actionRequiredRetries++;
      if (actionRequiredRetries <= MAX_ACTION_REQUIRED_RETRIES) {
        log.debug("Action required for response: {}", runResponse);
        ChatGptRunToolOutputHandler chatGptRunToolOutputHandler =
            new ChatGptRunToolOutputHandler(config, change, gitRepoFiles, chatGptRun);
        chatGptRunToolOutputHandler.submitToolOutput(getRunToolCalls(runResponse));
        runResponse.setStatus(null);
        return true;
      }
      log.debug("Max Action required retries reached: {}", actionRequiredRetries);
    }
    return false;
  }

  private List<ChatGptToolCall> getRunToolCalls(ChatGptRunResponse runResponse) {
    return runResponse.getRequiredAction().getSubmitToolOutputs().getToolCalls();
  }
}
