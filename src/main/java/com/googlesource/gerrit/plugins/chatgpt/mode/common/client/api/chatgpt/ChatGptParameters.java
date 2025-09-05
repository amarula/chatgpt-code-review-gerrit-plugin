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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ChatGptParameters extends ClientBase {
    private static boolean isCommentEvent;

    public ChatGptParameters(Configuration config, boolean isCommentEvent) {
        super(config);
        ChatGptParameters.isCommentEvent = isCommentEvent;
        log.debug("ChatGptParameters initialized with isCommentEvent: {}", isCommentEvent);
    }

    public double getGptTemperature() {
        log.debug("Getting GPT temperature");
        if (isCommentEvent) {
            return retrieveTemperature(Configuration.KEY_GPT_COMMENT_TEMPERATURE,
                    Configuration.DEFAULT_GPT_COMMENT_TEMPERATURE);
        }
        else {
            return retrieveTemperature(Configuration.KEY_GPT_REVIEW_TEMPERATURE,
                    Configuration.DEFAULT_GPT_REVIEW_TEMPERATURE);
        }
    }

    public boolean getStreamOutput() {
        return config.getGptStreamOutput() && !isCommentEvent;
    }

    public int getRandomSeed() {
        return ThreadLocalRandom.current().nextInt();
    }

    public boolean shouldSpecializeAssistants() {
        return config.getGptReviewCommitMessages() && config.getTaskSpecificAssistants();
    }

    private Double retrieveTemperature(String temperatureKey, Double defaultTemperature) {
        return Double.parseDouble(config.getString(temperatureKey, String.valueOf(defaultTemperature)));
    }
}
