package com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;

import java.util.concurrent.ThreadLocalRandom;

public class ChatGptParameters extends ClientBase {
    private static boolean isCommentEvent;

    public ChatGptParameters(Configuration config, boolean isCommentEvent) {
        super(config);
        ChatGptParameters.isCommentEvent = isCommentEvent;
    }

    public double getGptTemperature() {
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
