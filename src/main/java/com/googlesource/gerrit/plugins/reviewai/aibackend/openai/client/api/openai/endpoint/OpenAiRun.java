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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiPoller;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

@Slf4j
public class OpenAiRun extends OpenAiApiBase {
  private final String assistantId;
  private final String threadId;

  private String runId;

  public OpenAiRun(Configuration config, String assistantId, String threadId) {
    super(config);
    this.assistantId = assistantId;
    this.threadId = threadId;
  }

  public OpenAiRunResponse createRun() throws AiConnectionFailException {
    Request createRunRequest = createRunRequest();
    log.info("OpenAI Create Run request: {}", createRunRequest);

    OpenAiRunResponse runResponse = getOpenAiResponse(createRunRequest);
    log.info("Run created: {}", runResponse);
    runId = runResponse.getId();

    return runResponse;
  }

  public void cancelRun(String runId) {
    Request cancelRequest = getCancelRequest(runId);
    log.debug("OpenAI Cancel Run request: {}", cancelRequest);
    try {
      OpenAiResponse response = getOpenAiResponse(cancelRequest);
      if (!response.getStatus().equals(OpenAiPoller.CANCELLED_STATUS)) {
        log.error("Unable to cancel run. Run cancel response: {}", clientResponse);
      }
    } catch (Exception e) {
      log.error("Error cancelling run", e);
    }
  }

  public Request getStepsRequest(String runId) {
    String uri = OpenAiUriResourceLocator.runStepsUri(threadId, runId);
    log.debug("OpenAI Run Steps request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, null);
  }

  public void submitToolOutputs(List<OpenAiToolOutput> toolOutputs)
      throws AiConnectionFailException {
    Request submitToolOutputsRequest = submitToolOutputsRequest(toolOutputs);
    log.debug("OpenAI Submit Tool Outputs request: {}", submitToolOutputsRequest);

    OpenAiResponse submitToolOutputsResponse = getOpenAiResponse(submitToolOutputsRequest);
    log.debug("Submit Tool Outputs response: {}", submitToolOutputsResponse);
  }

  private Request createRunRequest() {
    String uri = OpenAiUriResourceLocator.runsUri(threadId);
    log.debug("OpenAI Create Run request URI: {}", uri);
    OpenAiCreateRunRequest requestBody =
        OpenAiCreateRunRequest.builder().assistantId(assistantId).build();

    return httpClient.createRequestFromJson(uri, requestBody);
  }

  private Request getCancelRequest(String runId) {
    String uri = OpenAiUriResourceLocator.runCancelUri(threadId, runId);
    log.debug("OpenAI Run Cancel request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, new Object());
  }

  private Request submitToolOutputsRequest(List<OpenAiToolOutput> toolOutputs) {
    String uri = OpenAiUriResourceLocator.runSubmitToolOutputsUri(threadId, runId);
    log.debug("OpenAI Submit Tool Outputs request URI: {}", uri);
    OpenAiSubmitToolOutputsToRunRequest submitToolOutputsToRunRequest =
        OpenAiSubmitToolOutputsToRunRequest.builder().toolOutputs(toolOutputs).build();
    log.debug("OpenAI Submit Tool Outputs request params: {}", submitToolOutputsToRunRequest);

    return httpClient.createRequestFromJson(uri, submitToolOutputsToRunRequest);
  }
}
