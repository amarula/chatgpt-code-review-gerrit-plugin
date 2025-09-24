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

import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiToolCall;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAiRun;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.ThreadUtils.threadSleep;

@Slf4j
public class OpenAiRunHandler extends OpenAiApiBase {
  private static final int STEP_RETRIEVAL_INTERVAL = 10000;
  private static final int MAX_STEP_RETRIEVAL_RETRIES = 3;

  private final ChangeSetData changeSetData;
  private final GerritChange change;
  private final String threadId;
  private final ICodeContextPolicy codeContextPolicy;
  private final PluginDataHandlerProvider pluginDataHandlerProvider;
  private final OpenAiPoller openAiPoller;

  private OpenAiRun openAiRun;
  private OpenAiRunResponse runResponse;
  private OpenAiListResponse stepResponse;

  public OpenAiRunHandler(
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
    openAiPoller = new OpenAiPoller(config);
  }

  public void setupRun() throws AiConnectionFailException {
    OpenAiAssistantHandler openAiAssistantHandler =
        new OpenAiAssistantHandler(
            config, changeSetData, change, codeContextPolicy, pluginDataHandlerProvider);
    openAiRun = new OpenAiRun(config, openAiAssistantHandler.setupAssistant(), threadId);
    runResponse = openAiRun.createRun();
  }

  public void pollRunStep() throws AiConnectionFailException {
    AiConnectionFailException exception = null;
    codeContextPolicy.setupRunAction(openAiRun);
    for (int retries = 0; retries < MAX_STEP_RETRIEVAL_RETRIES; retries++) {
      runResponse =
          openAiPoller.runPoll(
              OpenAiUriResourceLocator.runRetrieveUri(threadId, runResponse.getId()), runResponse);
      if (codeContextPolicy.runActionRequired(runResponse)) {
        continue;
      }
      Request stepsRequest = openAiRun.getStepsRequest(runResponse.getId());
      log.debug("OpenAI Retrieve Run Steps request: {}", stepsRequest);
      try {
        stepResponse = getOpenAiResponse(stepsRequest, OpenAiListResponse.class);
      } catch (AiConnectionFailException e) {
        exception = e;
        log.warn("Error retrieving run steps from OpenAI: {}", e.getMessage());
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      log.debug("OpenAI Response: {}", clientResponse);
      log.info(
          "Run executed after {} seconds ({} polling requests); Step response: {}",
          openAiPoller.getElapsedTime(),
          openAiPoller.getPollingCount(),
          stepResponse);
      if (stepResponse.getData().isEmpty()) {
        log.warn("Empty response from OpenAI");
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      return;
    }
    throw new AiConnectionFailException(exception);
  }

  public OpenAiResponseMessage getFirstStepDetails() {
    return getFirstStep().getStepDetails();
  }

  public List<AiToolCall> getFirstStepToolCalls() {
    return getFirstStepDetails().getToolCalls();
  }

  public void cancelRun() {
    if (getFirstStep().getStatus().equals(OpenAiPoller.COMPLETED_STATUS)) return;
    openAiRun.cancelRun(runResponse.getId());
  }

  private OpenAiRunStepsResponse getFirstStep() {
    return stepResponse.getData().get(0);
  }
}
