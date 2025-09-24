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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiToolCall;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAiRun;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OpenAiRunActionHandler extends ClientBase {
  private static final int MAX_ACTION_REQUIRED_RETRIES = 1;

  private final GerritChange change;
  private final GitRepoFiles gitRepoFiles;
  private final OpenAiRun openAiRun;

  private int actionRequiredRetries;

  public OpenAiRunActionHandler(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles, OpenAiRun openAiRun) {
    super(config);
    this.change = change;
    this.gitRepoFiles = gitRepoFiles;
    this.openAiRun = openAiRun;
    actionRequiredRetries = 0;
    log.debug("OpenAiRunActionHandler initialized");
  }

  public boolean runActionRequired(OpenAiRunResponse runResponse)
      throws AiConnectionFailException {
    log.debug("Response status: {}", runResponse.getStatus());
    if (OpenAiPoller.isActionRequired(runResponse.getStatus())) {
      actionRequiredRetries++;
      if (actionRequiredRetries <= MAX_ACTION_REQUIRED_RETRIES) {
        log.debug("Action required for response: {}", runResponse);
        OpenAiRunToolOutputHandler openAiRunToolOutputHandler =
            new OpenAiRunToolOutputHandler(config, change, gitRepoFiles, openAiRun);
        openAiRunToolOutputHandler.submitToolOutput(getRunToolCalls(runResponse));
        runResponse.setStatus(null);
        return true;
      }
      log.debug("Max Action required retries reached: {}", actionRequiredRetries);
    }
    return false;
  }

  private List<AiToolCall> getRunToolCalls(OpenAiRunResponse runResponse) {
    return runResponse.getRequiredAction().getSubmitToolOutputs().getToolCalls();
  }
}
