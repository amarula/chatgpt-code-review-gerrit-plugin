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
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.*;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.client.api.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.endpoint.ChatGptRun;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils.threadSleep;

@Slf4j
public class ChatGptRunHandler extends ChatGptApiBase {
  private static final int STEP_RETRIEVAL_INTERVAL = 10000;
  private static final int MAX_STEP_RETRIEVAL_RETRIES = 3;

  private final ChangeSetData changeSetData;
  private final GerritChange change;
  private final String threadId;
  private final ICodeContextPolicy codeContextPolicy;
  private final PluginDataHandlerProvider pluginDataHandlerProvider;
  private final ChatGptPoller chatGptPoller;

  private ChatGptRun chatGptRun;
  private ChatGptRunResponse runResponse;
  private ChatGptListResponse stepResponse;

  public ChatGptRunHandler(
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
    chatGptPoller = new ChatGptPoller(config);
  }

  public void setupRun() throws OpenAiConnectionFailException {
    ChatGptAssistantHandler chatGptAssistantHandler =
        new ChatGptAssistantHandler(
            config, changeSetData, change, codeContextPolicy, pluginDataHandlerProvider);
    chatGptRun = new ChatGptRun(config, chatGptAssistantHandler.setupAssistant(), threadId);
    runResponse = chatGptRun.createRun();
  }

  public void pollRunStep() throws OpenAiConnectionFailException {
    OpenAiConnectionFailException exception = null;
    codeContextPolicy.setupRunAction(chatGptRun);
    for (int retries = 0; retries < MAX_STEP_RETRIEVAL_RETRIES; retries++) {
      runResponse =
          chatGptPoller.runPoll(
              UriResourceLocator.runRetrieveUri(threadId, runResponse.getId()),
              runResponse);
      if (codeContextPolicy.runActionRequired(runResponse)) {
        continue;
      }
      Request stepsRequest = chatGptRun.getStepsRequest(runResponse.getId());
      log.debug("ChatGPT Retrieve Run Steps request: {}", stepsRequest);
      try {
        stepResponse = getChatGptResponse(stepsRequest, ChatGptListResponse.class);
      } catch (OpenAiConnectionFailException e) {
        exception = e;
        log.warn("Error retrieving run steps from ChatGPT: {}", e.getMessage());
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      log.debug("ChatGPT Response: {}", clientResponse);
      log.info(
          "Run executed after {} seconds ({} polling requests); Step response: {}",
          chatGptPoller.getElapsedTime(),
          chatGptPoller.getPollingCount(),
          stepResponse);
      if (stepResponse.getData().isEmpty()) {
        log.warn("Empty response from ChatGPT");
        threadSleep(STEP_RETRIEVAL_INTERVAL);
        continue;
      }
      return;
    }
    throw new OpenAiConnectionFailException(exception);
  }

  public ChatGptResponseMessage getFirstStepDetails() {
    return getFirstStep().getStepDetails();
  }

  public List<ChatGptToolCall> getFirstStepToolCalls() {
    return getFirstStepDetails().getToolCalls();
  }

  public void cancelRun() {
    if (getFirstStep().getStatus().equals(ChatGptPoller.COMPLETED_STATUS)) return;
    chatGptRun.cancelRun(runResponse.getId());
  }

  private ChatGptRunStepsResponse getFirstStep() {
    return stepResponse.getData().get(0);
  }
}
