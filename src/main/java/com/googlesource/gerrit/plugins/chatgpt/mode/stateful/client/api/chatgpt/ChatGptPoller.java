package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils.threadSleep;

@Slf4j
public class ChatGptPoller {
    public static final String COMPLETED_STATUS = "completed";
    public static final String CANCELLED_STATUS = "cancelled";
    public static final String FAILED_STATUS = "failed";

    private static final Set<String> PENDING_STATUSES = new HashSet<>(Arrays.asList(
            "queued",
            "in_progress",
            "cancelling"
    ));

    private final int pollingTimeout;
    private final int pollingInterval;
    private final ChatGptHttpClient httpClient;

    @Getter
    private int pollingCount;
    @Getter
    private double elapsedTime;

    public ChatGptPoller(Configuration config) {
        pollingTimeout = config.getGptPollingTimeout();
        pollingInterval = config.getGptPollingInterval();
        httpClient = new ChatGptHttpClient(config);
        elapsedTime = 0.0;
        pollingCount = 0;
    }

    public ChatGptResponse runPoll(String uri, ChatGptResponse pollResponse) throws OpenAiConnectionFailException {
        long startTime = TimeUtils.getCurrentMillis();

        while (isPending(pollResponse.getStatus())) {
            pollingCount++;
            log.debug("Polling request #{}", pollingCount);
            threadSleep(pollingInterval);
            Request pollRequest = httpClient.createRequestFromJson(uri, null);
            log.debug("ChatGPT Poll request: {}", pollRequest);
            pollResponse = getGson().fromJson(httpClient.execute(pollRequest), ChatGptResponse.class);
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

    private boolean isPending(String status) {
        return status == null || status.isEmpty() || PENDING_STATUSES.contains(status);
    }
}
