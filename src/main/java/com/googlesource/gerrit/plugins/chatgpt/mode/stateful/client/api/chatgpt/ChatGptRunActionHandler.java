package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ChatGptRunActionHandler extends ClientBase {
    private static final int MAX_ACTION_REQUIRED_RETRIES = 1;

    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final ChatGptRun chatGptRun;

    private int actionRequiredRetries;

    public ChatGptRunActionHandler(
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            ChatGptRun chatGptRun
    ) {
        super(config);
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        this.chatGptRun = chatGptRun;
        actionRequiredRetries = 0;
        log.debug("ChatGptRunActionHandler initialized");
    }

    public boolean runActionRequired(ChatGptRunResponse runResponse) throws OpenAiConnectionFailException {
        log.debug("Response status: {}", runResponse.getStatus());
        if (ChatGptPoller.isActionRequired(runResponse.getStatus())) {
            actionRequiredRetries++;
            if (actionRequiredRetries <= MAX_ACTION_REQUIRED_RETRIES) {
                log.debug("Action required for response: {}", runResponse);
                ChatGptRunToolOutputHandler chatGptRunToolOutputHandler = new ChatGptRunToolOutputHandler(
                        config,
                        change,
                        gitRepoFiles,
                        chatGptRun
                );
                chatGptRunToolOutputHandler.submitToolOutput(getRunToolCalls(runResponse));
                runResponse.setStatus(null);
                return true;
            }
            log.debug("Max Action required retries reached: {}", actionRequiredRetries);
        }
        return false;
    }

    private List<ChatGptToolCall> getRunToolCalls(ChatGptRunResponse runResponse) {
        return runResponse.getRequiredAction().getSubmitToolOutputs().getToolCalls();
    }
}
