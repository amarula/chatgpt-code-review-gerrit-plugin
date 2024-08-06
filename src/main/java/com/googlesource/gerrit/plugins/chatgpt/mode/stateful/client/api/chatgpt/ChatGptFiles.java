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
import java.net.URI;
import java.nio.file.Path;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptFiles extends ClientBase {
    private final HttpClient httpClient = new HttpClient();

    public ChatGptFiles(Configuration config) {
        super(config);
    }

    public ChatGptFilesResponse uploadFiles(Path repoPath) throws OpenAiConnectionFailException {
        Request request = createUploadFileRequest(repoPath);
        log.debug("ChatGPT Upload Files request: {}", request);

        String response = httpClient.execute(request);
        log.debug("ChatGPT Upload Files response: {}", response);

        return getGson().fromJson(response, ChatGptFilesResponse.class);
    }

    private Request createUploadFileRequest(Path repoPath) {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.filesCreateUri());
        log.debug("ChatGPT Upload Files request URI: {}", uri);
        File file = repoPath.toFile();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("purpose", "assistants")
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/json")))
                .build();

        return httpClient.createRequest(uri.toString(), config.getGptToken(), requestBody, null);
    }
}
