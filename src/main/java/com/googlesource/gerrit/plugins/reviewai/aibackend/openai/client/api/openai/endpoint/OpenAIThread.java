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
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAIApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
public class OpenAIThread extends OpenAIApiBase {
  public static final String KEY_THREAD_ID = "threadId";

  private final ChangeSetData changeSetData;
  private final PluginDataHandler changeDataHandler;

  public OpenAIThread(
      Configuration config,
      ChangeSetData changeSetData,
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    super(config);
    this.changeSetData = changeSetData;
    this.changeDataHandler = pluginDataHandlerProvider.getChangeScope();
  }

  public String createThread() throws OpenAiConnectionFailException {
    String threadId = changeDataHandler.getValue(KEY_THREAD_ID);
    if (threadId == null
        || !changeSetData.getForcedReview() && !changeSetData.getForcedStagedReview()) {
      Request request = createThreadRequest();
      log.debug("OpenAI Create Thread request: {}", request);

      OpenAIResponse threadResponse = getOpenAIResponse(request);
      threadId = threadResponse.getId();
      if (threadId != null) {
        log.info("Thread created: {}", threadResponse);
        changeDataHandler.setValue(KEY_THREAD_ID, threadId);
      } else {
        log.error("Failed to create thread. Response: {}", threadResponse);
      }
    } else {
      log.info("Existing thread found for the Change Set. Thread ID: {}", threadId);
    }
    return threadId;
  }

  private Request createThreadRequest() {
    String uri = OpenAIUriResourceLocator.threadsUri();
    log.debug("OpenAI Create Thread request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, new Object());
  }
}
