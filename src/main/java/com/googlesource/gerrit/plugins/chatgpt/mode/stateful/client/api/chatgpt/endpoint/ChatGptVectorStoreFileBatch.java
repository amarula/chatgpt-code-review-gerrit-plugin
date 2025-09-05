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
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptCreateVectorStoreRequest;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

@Slf4j
public class ChatGptVectorStoreFileBatch extends ChatGptApiBase {

    public ChatGptVectorStoreFileBatch(Configuration config) {
        super(config);
    }

    public ChatGptResponse createVectorStoreFileBatch(String vectorStoreId, List<String> fileIds)
            throws OpenAiConnectionFailException {
        Request request = vectorStoreFileBatchCreateRequest(vectorStoreId, fileIds);
        log.debug("ChatGPT Create Vector Store File Batch request: {}", request);

        ChatGptResponse createVectorStoreFileBatchResponse = getChatGptResponse(request);
        log.info("Vector Store File Batch created: {}", createVectorStoreFileBatchResponse);

        return createVectorStoreFileBatchResponse;
    }

    private Request vectorStoreFileBatchCreateRequest(String vectorStoreId, List<String> fileIds) {
        String uri = UriResourceLocatorStateful.vectorStoreFileBatchCreateUri(vectorStoreId);
        log.debug("ChatGPT Create Vector Store File Batch request URI: {}", uri);

        ChatGptCreateVectorStoreRequest requestBody = ChatGptCreateVectorStoreRequest.builder()
                .fileIds(fileIds.toArray(String[]::new))
                .build();

        log.debug("ChatGPT Create Vector Store File Batch request body: {}", requestBody);
        return httpClient.createRequestFromJson(uri, requestBody);
    }
}
