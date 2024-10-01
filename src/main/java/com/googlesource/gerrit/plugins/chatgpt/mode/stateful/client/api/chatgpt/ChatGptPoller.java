package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils.threadSleep;

@Slf4j
public class ChatGptPoller extends ChatGptApiBase {
    public static final String COMPLETED_STATUS = "completed";
    public static final String CANCELLED_STATUS = "cancelled";
    public static final String FAILED_STATUS = "failed";
    public static final String REQUIRES_ACTION_STATUS = "requires_action";

    private static final Set<String> PENDING_STATUSES = new HashSet<>(Arrays.asList(
            "queued",
            "in_progress",
            "cancelling"
    ));

    private final int pollingTimeout;
    private final int pollingInterval;

    @Getter
    private int pollingCount;
    @Getter
    private double elapsedTime;

    public ChatGptPoller(Configuration config) {
        super(config);
        pollingTimeout = config.getGptPollingTimeout();
        pollingInterval = config.getGptPollingInterval();
        elapsedTime = 0.0;
        pollingCount = 0;
    }

    public ChatGptRunResponse runPoll(String uri, ChatGptRunResponse pollResponse) throws OpenAiConnectionFailException {
        long startTime = TimeUtils.getCurrentMillis();

        while (isPending(pollResponse.getStatus())) {
            pollingCount++;
            log.debug("Polling request #{}", pollingCount);
            threadSleep(pollingInterval);
            Request pollRequest = httpClient.createRequestFromJson(uri, null);
            log.debug("ChatGPT Poll request: {}", pollRequest);
            pollResponse = getChatGptResponse(pollRequest);
            log.debug("ChatGPT Poll response: {}", pollResponse);
            elapsedTime = (double) (TimeUtils.getCurrentMillis() - startTime) / 1000;
            if (elapsedTime >= pollingTimeout) {
                log.error("Polling timed out after {} seconds.", elapsedTime);
                throw new OpenAiConnectionFailException();
            }
        }
        return pollResponse;
    }

    public boolean isNotCompleted(String status) {
        return status == null || !status.equals(COMPLETED_STATUS);
    }

    public boolean isActionRequired(String status) {
        return status != null && status.equals(REQUIRES_ACTION_STATUS);
    }

    private boolean isPending(String status) {
        return status == null || status.isEmpty() || PENDING_STATUSES.contains(status);
    }
}
