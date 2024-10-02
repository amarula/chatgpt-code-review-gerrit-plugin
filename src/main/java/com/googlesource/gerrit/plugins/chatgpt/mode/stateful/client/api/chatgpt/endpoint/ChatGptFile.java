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
