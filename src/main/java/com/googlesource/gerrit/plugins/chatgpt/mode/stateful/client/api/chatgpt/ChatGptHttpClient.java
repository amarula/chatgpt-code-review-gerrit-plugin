package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClient;
import okhttp3.Request;

import java.util.Map;

public class ChatGptHttpClient extends HttpClient {
    private static final Map<String, String> BETA_VERSION_HEADER = Map.of("OpenAI-Beta", "assistants=v2");

    public ChatGptHttpClient(Configuration config) {
        super(config);
    }

    @Override
    public Request createRequestFromJson(String uri, Object requestObject) {
        return createRequestFromJson(uri, requestObject, BETA_VERSION_HEADER);
    }
}
