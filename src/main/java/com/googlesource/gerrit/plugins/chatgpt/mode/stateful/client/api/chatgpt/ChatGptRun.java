package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;
import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistant.KEY_ASSISTANT_ID;
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

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final String threadId;
    private final PluginDataHandler projectDataHandler;

    private ChatGptResponse runResponse;
    private ChatGptListResponse stepResponse;

    public ChatGptRun(String threadId, Configuration config, PluginDataHandlerProvider pluginDataHandlerProvider) {
        super(config);
        this.threadId = threadId;
        this.projectDataHandler = pluginDataHandlerProvider.getProjectScope();
    }

    public void createRun() {
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
        return stepResponse.getData().get(0).getStepDetails();
    }

    public List<ChatGptToolCall> getFirstStep() {
        return getFirstStepDetails().getToolCalls();
    }

    private Request runCreateRequest() {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.runsUri(threadId));
        log.debug("ChatGPT Create Run request URI: {}", uri);

        ChatGptCreateRunRequest requestBody = ChatGptCreateRunRequest.builder()
                .assistantId(projectDataHandler.getValue(KEY_ASSISTANT_ID))
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

    private Request getRunPollRequest(URI uri) {
        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), null);
    }
}
