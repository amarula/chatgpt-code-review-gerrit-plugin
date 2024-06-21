package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.isJsonString;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.unwrapJsonCode;

@Slf4j
@Singleton
public class ChatGptClientStateful extends ChatGptClient implements IChatGptClient {
    private static final String TYPE_MESSAGE_CREATION = "message_creation";
    private static final String TYPE_TOOL_CALLS = "tool_calls";

    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;

    @VisibleForTesting
    @Inject
    public ChatGptClientStateful(
            Configuration config,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    }

    public ChatGptResponseContent ask(ChangeSetData changeSetData, GerritChange change, String patchSet) {
        isCommentEvent = change.getIsCommentEvent();
        String changeId = change.getFullChangeId();
        log.info("Processing STATEFUL ChatGPT Request with changeId: {}, Patch Set: {}", changeId, patchSet);

        ChatGptThread chatGptThread = new ChatGptThread(config, pluginDataHandlerProvider);
        String threadId = chatGptThread.createThread();

        ChatGptThreadMessage chatGptThreadMessage = new ChatGptThreadMessage(
                threadId,
                config,
                changeSetData,
                change,
                patchSet
        );
        chatGptThreadMessage.addMessage();

        ChatGptRun chatGptRun = new ChatGptRun(
                threadId,
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider,
                isCommentEvent
        );
        chatGptRun.createRun();
        chatGptRun.pollRun();
        // Attribute `requestBody` is valued for testing purposes
        requestBody = chatGptThreadMessage.getAddMessageRequestBody();
        log.debug("ChatGPT request body: {}", requestBody);

        ChatGptResponseContent chatGptResponseContent = getResponseContentStateful(threadId, chatGptRun);
        chatGptRun.cancelRun();

        return chatGptResponseContent;
    }

    private ChatGptResponseContent getResponseContentStateful(String threadId, ChatGptRun chatGptRun) {
        return switch (chatGptRun.getFirstStepDetails().getType()) {
            case TYPE_MESSAGE_CREATION -> retrieveThreadMessage(threadId, chatGptRun);
            case TYPE_TOOL_CALLS -> getResponseContent(chatGptRun.getFirstStepToolCalls());
            default -> throw new IllegalStateException("Unexpected Step Type in stateful ChatGpt response: " +
                    chatGptRun);
        };
    }

    private ChatGptResponseContent retrieveThreadMessage(String threadId, ChatGptRun chatGptRun) {
        ChatGptThreadMessage chatGptThreadMessage = new ChatGptThreadMessage(threadId, config);
        ChatGptThreadMessageResponse threadMessageResponse = chatGptThreadMessage.retrieveMessage(
                chatGptRun.getFirstStepDetails().getMessageCreation().getMessageId()
        );
        String responseText = threadMessageResponse.getContent().get(0).getText().getValue();
        if (responseText == null) {
            throw new RuntimeException("ChatGPT thread message response content is null");
        }
        if (isJsonString(responseText)) {
            return extractResponseContent(responseText);
        }
        return new ChatGptResponseContent(responseText);
    }

    private ChatGptResponseContent extractResponseContent(String responseText) {
        return getGson().fromJson(unwrapJsonCode(responseText), ChatGptResponseContent.class);
    }
}
