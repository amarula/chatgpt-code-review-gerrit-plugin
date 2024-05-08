package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptThread {
    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final Configuration config;
    private final GerritChange change;
    private final String patchSet;

    private String threadId;
    private ChatGptRequestMessage addMessageRequestBody;

    public ChatGptThread(Configuration config, GerritChange change, String patchSet) {
        this.config = config;
        this.change = change;
        this.patchSet = patchSet;
    }

    public String createThread() {
        Request request = createThreadRequest();
        log.debug("ChatGPT Create Thread request: {}", request);

        ChatGptResponse threadResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.info("Thread created: {}", threadResponse);
        threadId = threadResponse.getId();

        return threadId;
    }

    public void addMessage() {
        Request request = addMessageRequest();
        log.debug("ChatGPT Add Message request: {}", request);

        ChatGptResponse addMessageResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.info("Message added: {}", addMessageResponse);
    }

    public String getAddMessageRequestBody() {
        return getGson().toJson(addMessageRequestBody);
    }

    private Request createThreadRequest() {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.threadsUri());
        log.debug("ChatGPT Create Thread request URI: {}", uri);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), new Object());
    }

    private Request addMessageRequest() {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.threadMessagesUri(threadId));
        log.debug("ChatGPT Add Message request URI: {}", uri);
        ChatGptPromptStateful chatGptPromptStateful = new ChatGptPromptStateful(config, change);
        addMessageRequestBody = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateful.getDefaultGptThreadReviewMessage(patchSet))
                .build();

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), addMessageRequestBody);
    }

}
