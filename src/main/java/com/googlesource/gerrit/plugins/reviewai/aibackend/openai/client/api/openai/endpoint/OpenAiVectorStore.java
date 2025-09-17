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
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
public class OpenAiVectorStore extends OpenAiApiBase {
  private final GerritChange change;

  public OpenAiVectorStore(Configuration config, GerritChange change) {
    super(config);
    this.change = change;
  }

  public OpenAiResponse createVectorStore() throws AiConnectionFailException {
    Request request = vectorStoreCreateRequest();
    log.debug("OpenAI Create Vector Store request: {}", request);

    OpenAiResponse createVectorStoreResponse = getOpenAiResponse(request);
    log.info("Vector Store created: {}", createVectorStoreResponse);

    return createVectorStoreResponse;
  }

  private Request vectorStoreCreateRequest() {
    String uri = OpenAiUriResourceLocator.vectorStoreCreateUri();
    log.debug("OpenAI Create Vector Store request URI: {}", uri);

    OpenAiCreateVectorStoreRequest requestBody =
        OpenAiCreateVectorStoreRequest.builder().name(change.getProjectName()).build();

    log.debug("OpenAI Create Vector Store request body: {}", requestBody);
    return httpClient.createRequestFromJson(uri, requestBody);
  }
}
