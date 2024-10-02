package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils.threadSleep;

@Slf4j
public class ChatGptRunHandler extends ChatGptApiBase {
    private static final int STEP_RETRIEVAL_INTERVAL = 10000;
    private static final int MAX_STEP_RETRIEVAL_RETRIES = 3;

    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final String threadId;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final ChatGptPoller chatGptPoller;

    private ChatGptRun chatGptRun;
    private ChatGptResponse runResponse;
    private ChatGptListResponse stepResponse;

    public ChatGptRunHandler(
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
        chatGptPoller = new ChatGptPoller(config);
    }

    public void setupRun() throws OpenAiConnectionFailException {
        ChatGptAssistantHandler chatGptAssistantHandler = new ChatGptAssistantHandler(
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider
        );
        chatGptRun = new ChatGptRun(
                config,
                chatGptAssistantHandler.setupAssistant(),
                threadId
        );
        runResponse = chatGptRun.createRun();
    }

    public void pollRunStep() throws OpenAiConnectionFailException {
        OpenAiConnectionFailException exception = null;
        for (int retries = 0; retries < MAX_STEP_RETRIEVAL_RETRIES; retries++) {
            runResponse = chatGptPoller.runPoll(
                    UriResourceLocatorStateful.runRetrieveUri(threadId, runResponse.getId()),
                    runResponse
            );
            Request stepsRequest = chatGptRun.getStepsRequest(runResponse.getId());
            log.debug("ChatGPT Retrieve Run Steps request: {}", stepsRequest);
            try {
                stepResponse = getChatGptResponse(stepsRequest, ChatGptListResponse.class);
            } catch (OpenAiConnectionFailException e) {
                exception = e;
                log.warn("Error retrieving run steps from ChatGPT: {}", e.getMessage());
                threadSleep(STEP_RETRIEVAL_INTERVAL);
                continue;
            }
            log.debug("ChatGPT Response: {}", clientResponse);
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
        chatGptRun.cancelRun(runResponse.getId());
    }

    private ChatGptRunStepsResponse getFirstStep() {
        return stepResponse.getData().get(0);
    }
}
