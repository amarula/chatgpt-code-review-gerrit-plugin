package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
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

import java.net.URI;
import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptRun extends ClientBase {
    private static final int RUN_POLLING_INTERVAL = 1000;
    private static final Set<String> UNCOMPLETED_STATUSES = new HashSet<>(Arrays.asList(
            "queued",
            "in_progress",
            "cancelling"
    ));
    public static final String COMPLETED_STATUS = "completed";
    public static final String CANCELLED_STATUS = "cancelled";

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final String threadId;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final boolean isCommentEvent;

    private ChatGptResponse runResponse;
    private ChatGptListResponse stepResponse;
    private String keyAssistantId;

    public ChatGptRun(
            String threadId,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            boolean isCommentEvent
    ) {
        super(config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.threadId = threadId;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        this.isCommentEvent = isCommentEvent;
    }

    public void createRun() {
        ChatGptAssistantBase chatGptAssistant = isCommentEvent ?
             new ChatGptAssistantRequests(config, changeSetData, change, gitRepoFiles, pluginDataHandlerProvider) :
             new ChatGptAssistantReview(config, changeSetData, change, gitRepoFiles, pluginDataHandlerProvider);

        chatGptAssistant.setupAssistant();
        keyAssistantId = chatGptAssistant.getKeyAssistantId();

        Request request = runCreateRequest();
        log.info("ChatGPT Create Run request: {}", request);

        runResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.info("Run created: {}", runResponse);
    }

    public void pollRun() {
        int pollingCount = 0;

        while (UNCOMPLETED_STATUSES.contains(runResponse.getStatus())) {
            pollingCount++;
            log.debug("Polling request #{}", pollingCount);
            try {
                Thread.sleep(RUN_POLLING_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread was interrupted", e);
            }
            Request pollRequest = getPollRequest();
            log.debug("ChatGPT Poll Run request: {}", pollRequest);
            runResponse = getGson().fromJson(httpClient.execute(pollRequest), ChatGptResponse.class);
            log.debug("ChatGPT Run response: {}", runResponse);
        }
        Request stepsRequest = getStepsRequest();
        log.debug("ChatGPT Retrieve Run Steps request: {}", stepsRequest);

        String response = httpClient.execute(stepsRequest);
        stepResponse = getGson().fromJson(response, ChatGptListResponse.class);
        log.info("Run executed after {} polling requests: {}", pollingCount, stepResponse);
    }

    public ChatGptResponseMessage getFirstStepDetails() {
        return getFirstStep().getStepDetails();
    }

    public List<ChatGptToolCall> getFirstStepToolCalls() {
        return getFirstStepDetails().getToolCalls();
    }

    public void cancelRun() {
        if (getFirstStep().getStatus().equals(COMPLETED_STATUS)) return;

        Request cancelRequest = getCancelRequest();
        log.debug("ChatGPT Cancel Run request: {}", cancelRequest);
        try {
            String fullResponse = httpClient.execute(cancelRequest);
            log.debug("ChatGPT Cancel Run Full response: {}", fullResponse);
            ChatGptResponse response = getGson().fromJson(fullResponse, ChatGptResponse.class);
            if (!response.getStatus().equals(CANCELLED_STATUS)) {
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
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.runsUri(threadId));
        log.debug("ChatGPT Create Run request URI: {}", uri);

        PluginDataHandler projectDataHandler = pluginDataHandlerProvider.getProjectScope();
        ChatGptCreateRunRequest requestBody = ChatGptCreateRunRequest.builder()
                .assistantId(projectDataHandler.getValue(keyAssistantId))
                .build();

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), requestBody);
    }

    private Request getPollRequest() {
        URI uri = URI.create(config.getGptDomain()
                + UriResourceLocatorStateful.runRetrieveUri(threadId, runResponse.getId()));
        log.debug("ChatGPT Poll Run request URI: {}", uri);

        return getRunPollRequest(uri);
    }

    private Request getStepsRequest() {
        URI uri = URI.create(config.getGptDomain()
                + UriResourceLocatorStateful.runStepsUri(threadId, runResponse.getId()));
        log.debug("ChatGPT Run Steps request URI: {}", uri);

        return getRunPollRequest(uri);
    }

    private Request getCancelRequest() {
        URI uri = URI.create(config.getGptDomain()
                + UriResourceLocatorStateful.runCancelUri(threadId, runResponse.getId()));
        log.debug("ChatGPT Run Cancel request URI: {}", uri);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), new Object());
    }

    private Request getRunPollRequest(URI uri) {
        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), null);
    }
}
