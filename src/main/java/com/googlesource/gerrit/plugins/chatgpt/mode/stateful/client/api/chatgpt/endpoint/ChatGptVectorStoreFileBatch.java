package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
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
