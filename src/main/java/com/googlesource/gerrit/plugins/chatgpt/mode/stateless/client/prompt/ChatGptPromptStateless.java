package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public class ChatGptPromptStateless extends ChatGptPrompt {
    public static String DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION;
    public static String DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW;
    public static String DEFAULT_GPT_REVIEW_PROMPT;
    public static String DEFAULT_GPT_REVIEW_PROMPT_REVIEW;
    public static String DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY;
    public static String DEFAULT_GPT_REVIEW_PROMPT_DIFF;

    public ChatGptPromptStateless(Configuration config) {
        super(config);
        loadStatelessPrompts();
    }

    public ChatGptPromptStateless(Configuration config, boolean isCommentEvent) {
        super(config, isCommentEvent);
        loadStatelessPrompts();
    }

    public static String getDefaultGptReviewSystemPrompt() {
        return joinWithSpace(new ArrayList<>(List.of(
                DEFAULT_GPT_SYSTEM_PROMPT + DOT,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW
        )));
    }

    public String getGptSystemPrompt() {
        List<String> prompt = new ArrayList<>(Arrays.asList(
                config.getString(Configuration.KEY_GPT_SYSTEM_PROMPT, DEFAULT_GPT_SYSTEM_PROMPT) + DOT,
                ChatGptPromptStateless.DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION
        ));
        if (!isCommentEvent) {
            prompt.add(ChatGptPromptStateless.DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW);
        }
        return joinWithSpace(prompt);
    }

    public String getGptUserPrompt(ChangeSetData changeSetData, String patchSet) {
        List<String> prompt = new ArrayList<>();
        String gptRequestDataPrompt = changeSetData.getGptDataPrompt();
        boolean isValidRequestDataPrompt = gptRequestDataPrompt != null && !gptRequestDataPrompt.isEmpty();
        if (isCommentEvent && isValidRequestDataPrompt) {
            log.debug("Request User Prompt retrieved: {}", gptRequestDataPrompt);
            prompt.addAll(Arrays.asList(
                    DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                    patchSet,
                    DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                    gptRequestDataPrompt,
                    getCommentRequestPrompt(changeSetData.getCommentPropertiesSize())
            ));
        }
        else {
            prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT);
            prompt.addAll(getReviewSteps());
            prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_DIFF);
            prompt.add(patchSet);
            if (isValidRequestDataPrompt) {
                prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY);
                prompt.add(gptRequestDataPrompt);
            }
            if (!changeSetData.getDirectives().isEmpty()) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES);
                prompt.add(getNumberedListString(new ArrayList<>(changeSetData.getDirectives()), null, null));
            }
        }
        return joinWithNewLine(prompt);
    }

    private void loadStatelessPrompts() {
        // Avoid repeated loading of prompt constants
        if (DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION == null) {
            loadDefaultPrompts("promptsStateless");
        }
    }

    private List<String> getReviewSteps() {
        List<String> steps = new ArrayList<>(List.of(
                joinWithSpace(new ArrayList<>(List.of(
                        DEFAULT_GPT_REVIEW_PROMPT_REVIEW,
                        DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT,
                        getPatchSetReviewPrompt()
                )))
        ));
        if (config.getGptReviewCommitMessages()) {
            steps.add(getReviewPromptCommitMessages());
        }
        return steps;
    }
}
