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
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptFilesResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.nio.file.Path;

@Slf4j
public class ChatGptFile extends ChatGptApiBase {

    public ChatGptFile(Configuration config) {
        super(config);
    }

    public ChatGptFilesResponse uploadFile(Path repoPath) throws OpenAiConnectionFailException {
        Request request = createUploadFileRequest(repoPath);
        log.debug("ChatGPT Upload File request: {}", request);

        return getChatGptResponse(request, ChatGptFilesResponse.class);
    }

    private Request createUploadFileRequest(Path repoPath) {
        String uri = UriResourceLocatorStateful.filesCreateUri();
        log.debug("ChatGPT Upload File request URI: {}", uri);
        File file = repoPath.toFile();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("purpose", "assistants")
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/json")))
                .build();

        return httpClient.createRequest(uri, requestBody, null);
    }
}
