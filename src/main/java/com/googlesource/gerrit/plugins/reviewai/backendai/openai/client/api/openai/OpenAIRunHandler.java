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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint.OpenAIRun;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.ThreadUtils.threadSleep;

@Slf4j
public class OpenAIRunHandler extends OpenAIApiBase {
  private static final int STEP_RETRIEVAL_INTERVAL = 10000;
  private static final int MAX_STEP_RETRIEVAL_RETRIES = 3;

  private final ChangeSetData changeSetData;
  private final GerritChange change;
  private final String threadId;
  private final ICodeContextPolicy codeContextPolicy;
  private final PluginDataHandlerProvider pluginDataHandlerProvider;
  private final OpenAIPoller openAIPoller;

  private OpenAIRun openAIRun;
  private OpenAIRunResponse runResponse;
  private OpenAIListResponse stepResponse;

  public OpenAIRunHandler(
      String threadId,
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.changeSetData = changeSetData;
    this.change = change;
    this.threadId = threadId;
    this.codeContextPolicy = codeContextPolicy;
    this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    openAIPoller = new OpenAIPoller(config);
  }

  public void setupRun() throws OpenAiConnectionFailException {
    OpenAIAssistantHandler openAIAssistantHandler =
        new OpenAIAssistantHandler(
            config, changeSetData, change, codeContextPolicy, pluginDataHandlerProvider);
    openAIRun = new OpenAIRun(config, openAIAssistantHandler.setupAssistant(), threadId);
    runResponse = openAIRun.createRun();
  }

  public void pollRunStep() throws OpenAiConnectionFailException {
    OpenAiConnectionFailException exception = null;
    codeContextPolicy.setupRunAction(openAIRun);
    for (int retries = 0; retries < MAX_STEP_RETRIEVAL_RETRIES; retries++) {
      runResponse =
          openAIPoller.runPoll(
              OpenAIUriResourceLocator.runRetrieveUri(threadId, runResponse.getId()), runResponse);
      if (codeContextPolicy.runActionRequired(runResponse)) {
        continue;
      }
      Request stepsRequest = openAIRun.getStepsRequest(runResponse.getId());
      log.debug("OpenAI Retrieve Run Steps request: {}", stepsRequest);
      try {
        stepResponse = getOpenAIResponse(stepsRequest, OpenAIListResponse.class);
      } catch (OpenAiConnectionFailException e) {
        exception = e;
        log.warn("Error retrieving run steps from OpenAI: {}", e.getMessage());
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      log.debug("OpenAI Response: {}", clientResponse);
      log.info(
          "Run executed after {} seconds ({} polling requests); Step response: {}",
          openAIPoller.getElapsedTime(),
          openAIPoller.getPollingCount(),
          stepResponse);
      if (stepResponse.getData().isEmpty()) {
        log.warn("Empty response from OpenAI");
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      return;
    }
    throw new OpenAiConnectionFailException(exception);
  }

  public OpenAIResponseMessage getFirstStepDetails() {
    return getFirstStep().getStepDetails();
  }

  public List<OpenAIToolCall> getFirstStepToolCalls() {
    return getFirstStepDetails().getToolCalls();
  }

  public void cancelRun() {
    if (getFirstStep().getStatus().equals(OpenAIPoller.COMPLETED_STATUS)) return;
    openAIRun.cancelRun(runResponse.getId());
  }

  private OpenAIRunStepsResponse getFirstStep() {
    return stepResponse.getData().get(0);
  }
}
