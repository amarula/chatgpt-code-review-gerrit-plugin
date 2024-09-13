package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptFilesResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.nio.file.Path;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptFile extends ClientBase {
    private final HttpClient httpClient;

    public ChatGptFile(Configuration config) {
        super(config);
        httpClient = new HttpClient(config);
    }

    public ChatGptFilesResponse uploadFile(Path repoPath) throws OpenAiConnectionFailException {
        Request request = createUploadFileRequest(repoPath);
        log.debug("ChatGPT Upload File request: {}", request);

        String response = httpClient.execute(request);
        log.debug("ChatGPT Upload File response: {}", response);

        return getGson().fromJson(response, ChatGptFilesResponse.class);
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
