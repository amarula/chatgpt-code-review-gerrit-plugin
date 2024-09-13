package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;


import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptVectorStore extends ClientBase {
    public static final String KEY_VECTOR_STORE_ID = "vectorStoreId";

    private final ChatGptHttpClient httpClient;
    private final List<String> fileIds;
    private final GerritChange change;

    public ChatGptVectorStore(List<String> fileIds, Configuration config, GerritChange change) {
        super(config);
        this.fileIds = fileIds;
        this.change = change;
        httpClient = new ChatGptHttpClient(config);
    }

    public ChatGptResponse createVectorStore() throws OpenAiConnectionFailException {
        Request request = vectorStoreCreateRequest();
        log.debug("ChatGPT Create Vector Store request: {}", request);

        ChatGptResponse createVectorStoreResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.info("Vector Store created: {}", createVectorStoreResponse);

        return createVectorStoreResponse;
    }

    private Request vectorStoreCreateRequest() {
        String uri = UriResourceLocatorStateful.vectorStoreCreateUri();
        log.debug("ChatGPT Create Vector Store request URI: {}", uri);

        ChatGptCreateVectorStoreRequest requestBody = ChatGptCreateVectorStoreRequest.builder()
                .name(change.getProjectName())
                .fileIds(fileIds.toArray(String[]::new))
                .build();

        log.debug("ChatGPT Create Vector Store request body: {}", requestBody);
        return httpClient.createRequestFromJson(uri, requestBody);
    }
}
