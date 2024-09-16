package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils.threadSleep;

@Slf4j
public class ChatGptRun extends ClientBase {
    private static final int STEP_RETRIEVAL_INTERVAL = 10000;
    private static final int MAX_STEP_RETRIEVAL_RETRIES = 3;

    private final ChatGptHttpClient httpClient;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final String threadId;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final ChatGptPoller chatGptPoller;

    private ChatGptResponse runResponse;
    private ChatGptListResponse stepResponse;
    private String assistantId;

    public ChatGptRun(
            String threadId,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.threadId = threadId;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        httpClient = new ChatGptHttpClient(config);
        chatGptPoller = new ChatGptPoller(config);
    }

    public void createRun() throws OpenAiConnectionFailException {
        ChatGptAssistant chatGptAssistant = new ChatGptAssistant(
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider
        );
        assistantId = chatGptAssistant.setupAssistant();

        Request request = runCreateRequest();
        log.info("ChatGPT Create Run request: {}", request);

        runResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.info("Run created: {}", runResponse);
    }

    public void pollRunStep() throws OpenAiConnectionFailException {
        OpenAiConnectionFailException exception = null;
        for (int retries = 0; retries < MAX_STEP_RETRIEVAL_RETRIES; retries++) {
            runResponse = chatGptPoller.runPoll(
                    UriResourceLocatorStateful.runRetrieveUri(threadId, runResponse.getId()),
                    runResponse
            );
            Request stepsRequest = getStepsRequest();
            log.debug("ChatGPT Retrieve Run Steps request: {}", stepsRequest);

            String response;
            try {
                response = httpClient.execute(stepsRequest);
            } catch (OpenAiConnectionFailException e) {
                exception = e;
                log.warn("Error retrieving run steps from ChatGPT: {}", e.getMessage());
                threadSleep(STEP_RETRIEVAL_INTERVAL);
                continue;
            }
            log.debug("ChatGPT Response: {}", response);
            stepResponse = getGson().fromJson(response, ChatGptListResponse.class);
            log.info("Run executed after {} seconds ({} polling requests); Step response: {}",
                    chatGptPoller.getElapsedTime(), chatGptPoller.getPollingCount(), stepResponse);
            if (stepResponse.getData().isEmpty()) {
                log.warn("Empty response from ChatGPT");
                threadSleep(STEP_RETRIEVAL_INTERVAL);
                continue;
            }
            return;
        }
        throw new OpenAiConnectionFailException(exception);
    }

    public ChatGptResponseMessage getFirstStepDetails() {
        return getFirstStep().getStepDetails();
    }

    public List<ChatGptToolCall> getFirstStepToolCalls() {
        return getFirstStepDetails().getToolCalls();
    }

    public void cancelRun() {
        if (getFirstStep().getStatus().equals(ChatGptPoller.COMPLETED_STATUS)) return;

        Request cancelRequest = getCancelRequest();
        log.debug("ChatGPT Cancel Run request: {}", cancelRequest);
        try {
            String fullResponse = httpClient.execute(cancelRequest);
            log.debug("ChatGPT Cancel Run Full response: {}", fullResponse);
            ChatGptResponse response = getGson().fromJson(fullResponse, ChatGptResponse.class);
            if (!response.getStatus().equals(ChatGptPoller.CANCELLED_STATUS)) {
                log.error("Unable to cancel run. Run cancel response: {}", fullResponse);
            }
        }
        catch (Exception e) {
            log.error("Error cancelling run", e);
        }
    }

    private ChatGptRunStepsResponse getFirstStep() {
        return stepResponse.getData().get(0);
    }

    private Request runCreateRequest() {
        String uri = UriResourceLocatorStateful.runsUri(threadId);
        log.debug("ChatGPT Create Run request URI: {}", uri);
        ChatGptCreateRunRequest requestBody = ChatGptCreateRunRequest.builder()
                .assistantId(assistantId)
                .build();

        return httpClient.createRequestFromJson(uri, requestBody);
    }

    private Request getStepsRequest() {
        String uri = UriResourceLocatorStateful.runStepsUri(threadId, runResponse.getId());
        log.debug("ChatGPT Run Steps request URI: {}", uri);

        return httpClient.createRequestFromJson(uri, null);
    }

    private Request getCancelRequest() {
        String uri = UriResourceLocatorStateful.runCancelUri(threadId, runResponse.getId());
        log.debug("ChatGPT Run Cancel request URI: {}", uri);

        return httpClient.createRequestFromJson(uri, new Object());
    }
}
