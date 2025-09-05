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
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
public class ChatGptVectorStore extends ChatGptApiBase {
    private final GerritChange change;

    public ChatGptVectorStore(Configuration config, GerritChange change) {
        super(config);
        this.change = change;
    }

    public ChatGptResponse createVectorStore() throws OpenAiConnectionFailException {
        Request request = vectorStoreCreateRequest();
        log.debug("ChatGPT Create Vector Store request: {}", request);

        ChatGptResponse createVectorStoreResponse = getChatGptResponse(request);
        log.info("Vector Store created: {}", createVectorStoreResponse);

        return createVectorStoreResponse;
    }

    private Request vectorStoreCreateRequest() {
        String uri = UriResourceLocatorStateful.vectorStoreCreateUri();
        log.debug("ChatGPT Create Vector Store request URI: {}", uri);

        ChatGptCreateVectorStoreRequest requestBody = ChatGptCreateVectorStoreRequest.builder()
                .name(change.getProjectName())
                .build();

        log.debug("ChatGPT Create Vector Store request body: {}", requestBody);
        return httpClient.createRequestFromJson(uri, requestBody);
    }
}
