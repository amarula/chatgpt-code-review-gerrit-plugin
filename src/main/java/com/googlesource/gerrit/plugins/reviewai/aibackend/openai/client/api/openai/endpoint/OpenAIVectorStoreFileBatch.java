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
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAIApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAICreateVectorStoreRequest;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

@Slf4j
public class OpenAIVectorStoreFileBatch extends OpenAIApiBase {

  public OpenAIVectorStoreFileBatch(Configuration config) {
    super(config);
  }

  public OpenAIResponse createVectorStoreFileBatch(String vectorStoreId, List<String> fileIds)
      throws OpenAiConnectionFailException {
    Request request = vectorStoreFileBatchCreateRequest(vectorStoreId, fileIds);
    log.debug("OpenAI Create Vector Store File Batch request: {}", request);

    OpenAIResponse createVectorStoreFileBatchResponse = getOpenAIResponse(request);
    log.info("Vector Store File Batch created: {}", createVectorStoreFileBatchResponse);

    return createVectorStoreFileBatchResponse;
  }

  private Request vectorStoreFileBatchCreateRequest(String vectorStoreId, List<String> fileIds) {
    String uri = OpenAIUriResourceLocator.vectorStoreFileBatchCreateUri(vectorStoreId);
    log.debug("OpenAI Create Vector Store File Batch request URI: {}", uri);

    OpenAICreateVectorStoreRequest requestBody =
        OpenAICreateVectorStoreRequest.builder().fileIds(fileIds.toArray(String[]::new)).build();

    log.debug("OpenAI Create Vector Store File Batch request body: {}", requestBody);
    return httpClient.createRequestFromJson(uri, requestBody);
  }
}
