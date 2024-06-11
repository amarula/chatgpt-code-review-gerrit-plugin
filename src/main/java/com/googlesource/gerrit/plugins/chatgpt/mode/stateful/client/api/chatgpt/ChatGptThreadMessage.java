package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptThreadMessage extends ClientBase {
    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final String threadId;

    private ChangeSetData changeSetData;
    private GerritChange change;
    private String patchSet;
    private ChatGptRequestMessage addMessageRequestBody;

    public ChatGptThreadMessage(String threadId, Configuration config) {
        super(config);
        this.threadId = threadId;
    }

    public ChatGptThreadMessage(
            String threadId,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            String patchSet
    ) {
        this(threadId, config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.patchSet = patchSet;
    }

    public ChatGptThreadMessageResponse retrieveMessage(String messageId) {
        Request request = createRetrieveMessageRequest(messageId);
        log.debug("ChatGPT Retrieve Thread Message request: {}", request);
        ChatGptThreadMessageResponse threadMessageResponse = getGson().fromJson(
                httpClient.execute(request), ChatGptThreadMessageResponse.class
        );
        log.info("Thread Message retrieved: {}", threadMessageResponse);

        return threadMessageResponse;
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

    private Request createRetrieveMessageRequest(String messageId) {
        URI uri = URI.create(config.getGptDomain() +
                UriResourceLocatorStateful.threadMessageRetrieveUri(threadId, messageId));
        log.debug("ChatGPT Retrieve Thread Message request URI: {}", uri);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), null);
    }

    private Request addMessageRequest() {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.threadMessagesUri(threadId));
        log.debug("ChatGPT Add Message request URI: {}", uri);
        ChatGptPromptStateful chatGptPromptStateful = new ChatGptPromptStateful(config, changeSetData, change);
        addMessageRequestBody = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateful.getDefaultGptThreadReviewMessage(patchSet))
                .build();
        log.debug("ChatGPT Add Message request body: {}", addMessageRequestBody);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), addMessageRequestBody);
    }
}
