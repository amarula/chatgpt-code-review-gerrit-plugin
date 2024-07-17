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

    private Double retrieveTemperature(String temperatureKey, Double defaultTemperature) {
        return Double.parseDouble(config.getString(temperatureKey, String.valueOf(defaultTemperature)));
    }
}
