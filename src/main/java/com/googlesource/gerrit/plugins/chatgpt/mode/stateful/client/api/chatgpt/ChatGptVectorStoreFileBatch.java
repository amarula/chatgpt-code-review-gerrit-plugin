package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptCreateVectorStoreRequest;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptVectorStoreFileBatch extends ClientBase {
    private final ChatGptHttpClient httpClient;

    public ChatGptVectorStoreFileBatch(Configuration config) {
        super(config);
        httpClient = new ChatGptHttpClient(config);
    }

    public ChatGptResponse createVectorStoreFileBatch(String vectorStoreId, List<String> fileIds)
            throws OpenAiConnectionFailException {
        Request request = vectorStoreFileBatchCreateRequest(vectorStoreId, fileIds);
        log.debug("ChatGPT Create Vector Store File Batch request: {}", request);

        ChatGptResponse createVectorStoreFileBatchResponse = getGson().fromJson(httpClient.execute(request),
                ChatGptResponse.class);
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
