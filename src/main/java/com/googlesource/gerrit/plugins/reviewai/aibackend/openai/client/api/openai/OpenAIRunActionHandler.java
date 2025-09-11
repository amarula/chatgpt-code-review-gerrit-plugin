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
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIToolCall;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAIRun;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OpenAIRunActionHandler extends ClientBase {
  private static final int MAX_ACTION_REQUIRED_RETRIES = 1;

  private final GerritChange change;
  private final GitRepoFiles gitRepoFiles;
  private final OpenAIRun openAIRun;

  private int actionRequiredRetries;

  public OpenAIRunActionHandler(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles, OpenAIRun openAIRun) {
    super(config);
    this.change = change;
    this.gitRepoFiles = gitRepoFiles;
    this.openAIRun = openAIRun;
    actionRequiredRetries = 0;
    log.debug("OpenAIRunActionHandler initialized");
  }

  public boolean runActionRequired(OpenAIRunResponse runResponse)
      throws OpenAiConnectionFailException {
    log.debug("Response status: {}", runResponse.getStatus());
    if (OpenAIPoller.isActionRequired(runResponse.getStatus())) {
      actionRequiredRetries++;
      if (actionRequiredRetries <= MAX_ACTION_REQUIRED_RETRIES) {
        log.debug("Action required for response: {}", runResponse);
        OpenAIRunToolOutputHandler openAIRunToolOutputHandler =
            new OpenAIRunToolOutputHandler(config, change, gitRepoFiles, openAIRun);
        openAIRunToolOutputHandler.submitToolOutput(getRunToolCalls(runResponse));
        runResponse.setStatus(null);
        return true;
      }
      log.debug("Max Action required retries reached: {}", actionRequiredRetries);
    }
    return false;
  }

  private List<OpenAIToolCall> getRunToolCalls(OpenAIRunResponse runResponse) {
    return runResponse.getRequiredAction().getSubmitToolOutputs().getToolCalls();
  }
}
