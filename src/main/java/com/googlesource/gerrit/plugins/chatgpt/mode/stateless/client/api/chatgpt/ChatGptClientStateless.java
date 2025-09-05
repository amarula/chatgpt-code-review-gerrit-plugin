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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.chatgpt;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.*;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.model.api.chatgpt.ChatGptCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getNoEscapedGson;

@Slf4j
@Singleton
public class ChatGptClientStateless extends ChatGptClient implements IChatGptClient {
    private static final int REVIEW_ATTEMPT_LIMIT = 3;

    private final HttpClient httpClient;

    @VisibleForTesting
    @Inject
    public ChatGptClientStateless(Configuration config) {
        super(config);
        httpClient = new HttpClient(config);
    }

    public ChatGptResponseContent ask(ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws Exception {
        isCommentEvent = change.getIsCommentEvent();
        String changeId = change.getFullChangeId();
        log.info("Processing STATELESS ChatGPT Request with changeId: {}, Patch Set: {}", changeId, patchSet);
        for (int attemptInd = 0; attemptInd < REVIEW_ATTEMPT_LIMIT; attemptInd++) {
            Request request = createRequest(config, changeSetData, patchSet);
            log.debug("ChatGPT request attempt #{}: {}", attemptInd, request);

            String response = httpClient.execute(request);
            log.debug("ChatGPT response body attempt #{}: {}", attemptInd, response);
            if (response == null) {
                throw new IOException("ChatGPT response body is null");
            }

            ChatGptResponseContent contentExtracted = extractContent(config, response);
            if (validateResponse(contentExtracted, changeId, attemptInd)) {
                log.info("Valid ChatGPT response received on attempt #{}", attemptInd);
                return contentExtracted;
            }
            else {
                log.warn("Invalid ChatGPT response on attempt #{}", attemptInd);
            }
        }
        log.error("Failed to receive valid ChatGPT response after {} attempts", REVIEW_ATTEMPT_LIMIT);
        throw new RuntimeException("Failed to receive valid ChatGPT response");
    }

    protected Request createRequest(Configuration config, ChangeSetData changeSetData, String patchSet) {
        String uri = UriResourceLocatorStateless.chatCompletionsUri();
        log.debug("ChatGPT request URI: {}", uri);
        ChatGptCompletionRequest completionRequest = createRequestBody(config, changeSetData, patchSet);

        return httpClient.createRequestFromJson(uri, completionRequest);
    }

    private ChatGptCompletionRequest createRequestBody(Configuration config, ChangeSetData changeSetData, String patchSet) {
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, isCommentEvent);
        ChatGptRequestMessage systemMessage = ChatGptRequestMessage.builder()
                .role("system")
                .content(chatGptPromptStateless.getGptSystemPrompt())
                .build();
        ChatGptRequestMessage userMessage = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateless.getGptUserPrompt(changeSetData, patchSet))
                .build();

        log.debug("System message for ChatGPT: {}", systemMessage.getContent());
        log.debug("User message for ChatGPT: {}", userMessage.getContent());

        ChatGptParameters chatGptParameters = new ChatGptParameters(config, isCommentEvent);
        ChatGptTools chatGptTools = new ChatGptTools(ChatGptTools.Functions.formatReplies);
        ChatGptTool[] tools = new ChatGptTool[] {
                chatGptTools.retrieveFunctionTool()
        };
        ChatGptCompletionRequest chatGptCompletionRequest = ChatGptCompletionRequest.builder()
                .model(config.getGptModel())
                .messages(List.of(systemMessage, userMessage))
                .temperature(chatGptParameters.getGptTemperature())
                .stream(chatGptParameters.getStreamOutput())
                // Seed value is Utilized to prevent ChatGPT from mixing up separate API calls that occur in close
                // temporal proximity.
                .seed(chatGptParameters.getRandomSeed())
                .tools(tools)
                .toolChoice(chatGptTools.retrieveFunctionToolChoice())
                .build();

        requestBody = getNoEscapedGson().toJson(chatGptCompletionRequest);
        log.debug("ChatGPT request body: {}", requestBody);

        return chatGptCompletionRequest;
    }
}
