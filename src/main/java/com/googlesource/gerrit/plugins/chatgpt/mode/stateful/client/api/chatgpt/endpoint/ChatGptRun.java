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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptPoller;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

@Slf4j
public class ChatGptRun extends ChatGptApiBase {
  private final String assistantId;
  private final String threadId;

  private String runId;

  public ChatGptRun(Configuration config, String assistantId, String threadId) {
    super(config);
    this.assistantId = assistantId;
    this.threadId = threadId;
  }

  public ChatGptRunResponse createRun() throws OpenAiConnectionFailException {
    Request createRunRequest = createRunRequest();
    log.info("ChatGPT Create Run request: {}", createRunRequest);

    ChatGptRunResponse runResponse = getChatGptResponse(createRunRequest);
    log.info("Run created: {}", runResponse);
    runId = runResponse.getId();

    return runResponse;
  }

  public void cancelRun(String runId) {
    Request cancelRequest = getCancelRequest(runId);
    log.debug("ChatGPT Cancel Run request: {}", cancelRequest);
    try {
      ChatGptResponse response = getChatGptResponse(cancelRequest);
      if (!response.getStatus().equals(ChatGptPoller.CANCELLED_STATUS)) {
        log.error("Unable to cancel run. Run cancel response: {}", clientResponse);
      }
    } catch (Exception e) {
      log.error("Error cancelling run", e);
    }
  }

  public Request getStepsRequest(String runId) {
    String uri = UriResourceLocatorStateful.runStepsUri(threadId, runId);
    log.debug("ChatGPT Run Steps request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, null);
  }

  public void submitToolOutputs(List<ChatGptToolOutput> toolOutputs)
      throws OpenAiConnectionFailException {
    Request submitToolOutputsRequest = submitToolOutputsRequest(toolOutputs);
    log.debug("ChatGPT Submit Tool Outputs request: {}", submitToolOutputsRequest);

    ChatGptResponse submitToolOutputsResponse = getChatGptResponse(submitToolOutputsRequest);
    log.debug("Submit Tool Outputs response: {}", submitToolOutputsResponse);
  }

  private Request createRunRequest() {
    String uri = UriResourceLocatorStateful.runsUri(threadId);
    log.debug("ChatGPT Create Run request URI: {}", uri);
    ChatGptCreateRunRequest requestBody =
        ChatGptCreateRunRequest.builder().assistantId(assistantId).build();

    return httpClient.createRequestFromJson(uri, requestBody);
  }

  private Request getCancelRequest(String runId) {
    String uri = UriResourceLocatorStateful.runCancelUri(threadId, runId);
    log.debug("ChatGPT Run Cancel request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, new Object());
  }

  private Request submitToolOutputsRequest(List<ChatGptToolOutput> toolOutputs) {
    String uri = UriResourceLocatorStateful.runSubmitToolOutputsUri(threadId, runId);
    log.debug("ChatGPT Submit Tool Outputs request URI: {}", uri);
    ChatGptSubmitToolOutputsToRunRequest submitToolOutputsToRunRequest =
        ChatGptSubmitToolOutputsToRunRequest.builder().toolOutputs(toolOutputs).build();
    log.debug("ChatGPT Submit Tool Outputs request params: {}", submitToolOutputsToRunRequest);

    return httpClient.createRequestFromJson(uri, submitToolOutputsToRunRequest);
  }
}
