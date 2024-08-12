package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.isJsonObjectAsString;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.unwrapJsonCode;

@Slf4j
@Singleton
public class ChatGptClientStateful extends ChatGptClient implements IChatGptClient {
    public enum ReviewAssistantStages {
        REVIEW_CODE,
        REVIEW_COMMIT_MESSAGE,
        REVIEW_REITERATED
    }
    private static final String TYPE_MESSAGE_CREATION = "message_creation";
    private static final String TYPE_TOOL_CALLS = "tool_calls";
    private static final int MAX_REITERATION_REQUESTS = 2;

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
        log.debug("Initialized ChatGptClientStateful.");
    }

    public ChatGptResponseContent ask(ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws OpenAiConnectionFailException {
        isCommentEvent = change.getIsCommentEvent();
        log.info("Processing STATEFUL ChatGPT Request with changeId: {}, Patch Set: {}", change.getFullChangeId(),
                patchSet);

        ChatGptResponseContent chatGptResponseContent = null;
        for (int reiterate = 0; reiterate < MAX_REITERATION_REQUESTS; reiterate++) {
            chatGptResponseContent = askSingleRequest(changeSetData, change, patchSet);
            if (chatGptResponseContent == null) {
                return null;
            }
            if (!isCommentEvent && chatGptResponseContent.getReplies() == null) {
                log.debug("Review response in incorrect format; Requesting resend with correct format.");
                changeSetData.setForcedStagedReview(true);
                changeSetData.setReviewAssistantStage(ReviewAssistantStages.REVIEW_REITERATED);
            }
            else {
                break;
            }
        }
        return chatGptResponseContent;
    }

    private ChatGptResponseContent askSingleRequest(ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws OpenAiConnectionFailException {
        log.debug("Processing Single Stateful Request");
        String threadId = createThreadWithMessage(changeSetData, change, patchSet);
        ChatGptRun chatGptRun = runThread(changeSetData, change, threadId);
        ChatGptResponseContent chatGptResponseContent = getResponseContentStateful(threadId, chatGptRun);
        chatGptRun.cancelRun();

        return chatGptResponseContent;
    }

    private String createThreadWithMessage(ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws OpenAiConnectionFailException {
        ChatGptThread chatGptThread = new ChatGptThread(config, changeSetData, pluginDataHandlerProvider);
        String threadId = chatGptThread.createThread();
        log.debug("Created ChatGPT thread with ID: {}", threadId);

        ChatGptThreadMessage chatGptThreadMessage = new ChatGptThreadMessage(
                threadId,
                config,
                changeSetData,
                change,
                patchSet
        );
        chatGptThreadMessage.addMessage();

        requestBody = chatGptThreadMessage.getAddMessageRequestBody();  // Valued for testing purposes
        log.debug("ChatGPT request body: {}", requestBody);

        return threadId;
    }

    private ChatGptRun runThread(ChangeSetData changeSetData, GerritChange change, String threadId)
            throws OpenAiConnectionFailException {
        ChatGptRun chatGptRun = new ChatGptRun(
                threadId,
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider
        );
        chatGptRun.createRun();
        chatGptRun.pollRunStep();

        return chatGptRun;
    }

    private ChatGptResponseContent getResponseContentStateful(String threadId, ChatGptRun chatGptRun)
            throws OpenAiConnectionFailException {
        return switch (chatGptRun.getFirstStepDetails().getType()) {
            case TYPE_MESSAGE_CREATION -> {
                log.debug("Retrieving thread message for thread ID: {}", threadId);
                yield retrieveThreadMessage(threadId, chatGptRun);
            }
            case TYPE_TOOL_CALLS -> {
                log.debug("Processing tool calls from ChatGPT run.");
                yield getResponseContent(chatGptRun.getFirstStepToolCalls());
            }
            default -> throw new IllegalStateException("Unexpected Step Type in stateful ChatGpt response: " + chatGptRun);
        };
    }

    private ChatGptResponseContent retrieveThreadMessage(String threadId, ChatGptRun chatGptRun)
            throws OpenAiConnectionFailException {
        ChatGptThreadMessage chatGptThreadMessage = new ChatGptThreadMessage(threadId, config);
        String messageId = chatGptRun.getFirstStepDetails().getMessageCreation().getMessageId();
        log.debug("Retrieving message with ID: {}", messageId);

        ChatGptThreadMessageResponse threadMessageResponse = chatGptThreadMessage.retrieveMessage(messageId);
        String responseText = threadMessageResponse.getContent().get(0).getText().getValue();
        if (responseText == null) {
            log.error("ChatGPT thread message response content is null for message ID: {}", messageId);
            throw new RuntimeException("ChatGPT thread message response content is null");
        }

        log.debug("Response text received: {}", responseText);
        if (isJsonObjectAsString(responseText)) {
            log.debug("Response text is JSON, extracting content.");
            return extractResponseContent(responseText);
        }

        log.debug("Response text is not JSON, returning as is.");
        return new ChatGptResponseContent(responseText);
    }

    private ChatGptResponseContent extractResponseContent(String responseText) {
        log.debug("Extracting response content from JSON.");
        return getGson().fromJson(unwrapJsonCode(responseText), ChatGptResponseContent.class);
    }
}
