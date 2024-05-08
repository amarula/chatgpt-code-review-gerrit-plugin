package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClient;
import okhttp3.Request;

import java.util.Map;

public class ChatGptHttpClient extends HttpClient {
    private static final Map<String, String> BETA_VERSION_HEADER = Map.of("OpenAI-Beta", "assistants=v1");

    public Request createRequestFromJson(String uri, String bearer, Object requestObject) {
        return createRequestFromJson(uri, bearer, requestObject, BETA_VERSION_HEADER);
    }

}
