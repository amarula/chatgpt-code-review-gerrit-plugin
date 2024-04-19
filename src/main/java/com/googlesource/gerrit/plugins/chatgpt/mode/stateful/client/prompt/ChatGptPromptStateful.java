package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ChatGptPromptStateful extends ChatGptPrompt {
    public static String DEFAULT_GPT_ASSISTANT_NAME;
    public static String DEFAULT_GPT_ASSISTANT_DESCRIPTION;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS;

    private final GerritChange change;

    public ChatGptPromptStateful(Configuration config, GerritChange change) {
        super(config);
        this.change = change;
        this.isCommentEvent = change.getIsCommentEvent();
        // Avoid repeated loading of prompt constants
        if (DEFAULT_GPT_ASSISTANT_NAME == null) {
            loadPrompts("promptsStateful");
        }
    }

    public String getDefaultGptAssistantDescription() {
        return String.format(DEFAULT_GPT_ASSISTANT_DESCRIPTION, change.getProjectName());
    }

    public String getDefaultGptAssistantInstructions() {
        return DEFAULT_GPT_SYSTEM_PROMPT + DOT +
                String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS, change.getProjectName()) +
                getPatchSetReviewUserPrompt();
    }

}
