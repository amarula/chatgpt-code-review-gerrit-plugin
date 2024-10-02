package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptPoller;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptCreateRunRequest;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

@Slf4j
public class ChatGptRun extends ChatGptApiBase {
    private final String assistantId;
    private final String threadId;

    public ChatGptRun(Configuration config, String assistantId, String threadId) {
        super(config);
        this.assistantId = assistantId;
        this.threadId = threadId;
    }

    public ChatGptResponse createRun() throws OpenAiConnectionFailException {
        Request request = createRunRequest();
        log.info("ChatGPT Create Run request: {}", request);

        ChatGptResponse runResponse = getChatGptResponse(request);
        log.info("Run created: {}", runResponse);

        return runResponse;
    }

    public void cancelRun(String runId) {
        Request cancelRequest = getCancelRequest(runId);
        log.debug("ChatGPT Cancel Run request: {}", cancelRequest);
        try {
            ChatGptResponse response = getChatGptResponse(cancelRequest);
            if (!response.getStatus().equals(ChatGptPoller.CANCELLED_STATUS)) {
                log.error("Unable to cancel run. Run cancel response: {}", clientResponse);
            }
        }
        catch (Exception e) {
            log.error("Error cancelling run", e);
        }
    }

    public Request getStepsRequest(String runId) {
        String uri = UriResourceLocatorStateful.runStepsUri(threadId, runId);
        log.debug("ChatGPT Run Steps request URI: {}", uri);

        return httpClient.createRequestFromJson(uri, null);
    }

    private Request createRunRequest() {
        String uri = UriResourceLocatorStateful.runsUri(threadId);
        log.debug("ChatGPT Create Run request URI: {}", uri);
        ChatGptCreateRunRequest requestBody = ChatGptCreateRunRequest.builder()
                .assistantId(assistantId)
                .build();

        return httpClient.createRequestFromJson(uri, requestBody);
    }

    private Request getCancelRequest(String runId) {
        String uri = UriResourceLocatorStateful.runCancelUri(threadId, runId);
        log.debug("ChatGPT Run Cancel request URI: {}", uri);

        return httpClient.createRequestFromJson(uri, new Object());
    }
}
