package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
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
