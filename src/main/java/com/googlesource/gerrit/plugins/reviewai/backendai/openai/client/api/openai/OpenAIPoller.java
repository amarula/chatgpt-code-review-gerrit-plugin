/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIRunResponse;
import com.googlesource.gerrit.plugins.reviewai.utils.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.googlesource.gerrit.plugins.reviewai.utils.ThreadUtils.threadSleep;

@Slf4j
public class OpenAIPoller extends OpenAIApiBase {
  public static final String COMPLETED_STATUS = "completed";
  public static final String CANCELLED_STATUS = "cancelled";
  public static final String FAILED_STATUS = "failed";
  public static final String REQUIRES_ACTION_STATUS = "requires_action";

  private static final Set<String> PENDING_STATUSES =
      new HashSet<>(Arrays.asList("queued", "in_progress", "cancelling"));

  private final int pollingTimeout;
  private final int pollingInterval;

  @Getter private int pollingCount;
  @Getter private double elapsedTime;

  public OpenAIPoller(Configuration config) {
    super(config);
    pollingTimeout = config.getGptPollingTimeout();
    pollingInterval = config.getGptPollingInterval();
    elapsedTime = 0.0;
    pollingCount = 0;
  }

  public OpenAIRunResponse runPoll(String uri, OpenAIRunResponse pollResponse)
      throws OpenAiConnectionFailException {
    long startTime = TimeUtils.getCurrentMillis();

    while (isPending(pollResponse.getStatus())) {
      pollingCount++;
      log.debug("Polling request #{}", pollingCount);
      threadSleep(pollingInterval);
      Request pollRequest = httpClient.createRequestFromJson(uri, null);
      log.debug("OpenAI Poll request: {}", pollRequest);
      pollResponse = getOpenAIResponse(pollRequest);
      log.debug("OpenAI Poll response: {}", pollResponse);
      elapsedTime = (double) (TimeUtils.getCurrentMillis() - startTime) / 1000;
      if (elapsedTime >= pollingTimeout) {
        log.error("Polling timed out after {} seconds.", elapsedTime);
        throw new OpenAiConnectionFailException();
      }
    }
    return pollResponse;
  }

  public static boolean isNotCompleted(String status) {
    return status == null || !status.equals(COMPLETED_STATUS);
  }

  public static boolean isActionRequired(String status) {
    return status != null && status.equals(REQUIRES_ACTION_STATUS);
  }

  private static boolean isPending(String status) {
    return status == null || status.isEmpty() || PENDING_STATUSES.contains(status);
  }
}
