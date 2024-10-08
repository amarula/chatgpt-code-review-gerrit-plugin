package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptThreadMessage extends ChatGptApiBase {
    private final String threadId;

    private ChangeSetData changeSetData;
    private GerritChange change;
    private ICodeContextPolicy codeContextPolicy;
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
            ICodeContextPolicy codeContextPolicy,
            String patchSet
    ) {
        this(threadId, config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.codeContextPolicy = codeContextPolicy;
        this.patchSet = patchSet;
    }

    public ChatGptThreadMessageResponse retrieveMessage(String messageId) throws OpenAiConnectionFailException {
        Request request = createRetrieveMessageRequest(messageId);
        log.debug("ChatGPT Retrieve Thread Message request: {}", request);
        ChatGptThreadMessageResponse threadMessageResponse = getChatGptResponse(
                request, ChatGptThreadMessageResponse.class
        );
        log.info("Thread Message retrieved: {}", threadMessageResponse);

        return threadMessageResponse;
    }

    public void addMessage() throws OpenAiConnectionFailException {
        Request request = addMessageRequest();
        log.debug("ChatGPT Add Message request: {}", request);

        ChatGptResponse addMessageResponse = getChatGptResponse(request);
        log.info("Message added: {}", addMessageResponse);
    }

    public String getAddMessageRequestBody() {
        return getGson().toJson(addMessageRequestBody);
    }

    private Request createRetrieveMessageRequest(String messageId) {
        String uri = UriResourceLocatorStateful.threadMessageRetrieveUri(threadId, messageId);
        log.debug("ChatGPT Retrieve Thread Message request URI: {}", uri);

        return httpClient.createRequestFromJson(uri, null);
    }

    private Request addMessageRequest() {
        String uri = UriResourceLocatorStateful.threadMessagesUri(threadId);
        log.debug("ChatGPT Add Message request URI: {}", uri);
        IChatGptPromptStateful chatGptPromptStateful = getChatGptPromptStateful(
                config,
                changeSetData,
                change,
                codeContextPolicy
        );
        addMessageRequestBody = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateful.getDefaultGptThreadReviewMessage(patchSet))
                .build();
        log.debug("ChatGPT Add Message request body: {}", addMessageRequestBody);

        return httpClient.createRequestFromJson(uri, addMessageRequestBody);
    }
}
