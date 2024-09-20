package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public abstract class ChatGptApiBase extends ClientBase {
    protected final ChatGptHttpClient httpClient;

    protected String clientResponse;

    public ChatGptApiBase(Configuration config) {
        super(config);
        httpClient = new ChatGptHttpClient(config);
    }

    public <T> T getChatGptResponse(Request request, Class<T> clazz) throws OpenAiConnectionFailException {
        log.debug("ChatGPT Client request: {}", request);
        clientResponse = httpClient.execute(request);
        log.debug("ChatGPT Client response: {}", clientResponse);

        return getGson().fromJson(clientResponse, clazz);
    }

    public ChatGptResponse getChatGptResponse(Request request) throws OpenAiConnectionFailException {
        return getChatGptResponse(request, ChatGptResponse.class);
    }
}
