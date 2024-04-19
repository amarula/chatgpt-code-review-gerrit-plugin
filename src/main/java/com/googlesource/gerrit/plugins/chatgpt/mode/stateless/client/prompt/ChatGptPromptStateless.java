package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.concatenate;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.getNumberedListString;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

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
        return DEFAULT_GPT_SYSTEM_PROMPT + DOT +
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION + SPACE +
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW;
    }

    public String getGptSystemPrompt() {
        List<String> prompt = new ArrayList<>(Arrays.asList(
                config.getString(Configuration.KEY_GPT_SYSTEM_PROMPT, DEFAULT_GPT_SYSTEM_PROMPT), DOT,
                ChatGptPromptStateless.DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION
        ));
        if (!isCommentEvent) {
            prompt.addAll(Arrays.asList(SPACE, ChatGptPromptStateless.DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW));
        }
        return concatenate(prompt);
    }

    public String getGptUserPrompt(String patchSet, String changeId) {
        List<String> prompt = new ArrayList<>();
        ChangeSetData changeSetData = ChangeSetDataHandler.getInstance(changeId);
        String gptRequestUserPrompt = changeSetData.getGptRequestUserPrompt();
        boolean isValidRequestUserPrompt = gptRequestUserPrompt != null && !gptRequestUserPrompt.isEmpty();
        if (isCommentEvent && isValidRequestUserPrompt) {
            log.debug("ConfigsDynamically value found: {}", gptRequestUserPrompt);
            prompt.addAll(Arrays.asList(
                    DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                    patchSet,
                    DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                    gptRequestUserPrompt,
                    getCommentRequestUserPrompt(changeSetData.getCommentPropertiesSize())
            ));
        }
        else {
            prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT);
            prompt.addAll(getReviewSteps());
            prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_DIFF);
            prompt.add(patchSet);
            if (isValidRequestUserPrompt) {
                prompt.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY);
                prompt.add(gptRequestUserPrompt);
            }
            if (!changeSetData.getDirectives().isEmpty()) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES);
                prompt.add(getNumberedListString(new ArrayList<>(changeSetData.getDirectives())));
            }
        }
        return joinWithNewLine(prompt);
    }

    private void loadStatelessPrompts() {
        // Avoid repeated loading of prompt constants
        if (DEFAULT_GPT_REVIEW_PROMPT == null) {
            loadPrompts("promptsStateless");
        }
    }

    private List<String> getReviewSteps() {
        List<String> steps = new ArrayList<>(){};
        steps.add(ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_REVIEW + SPACE + getPatchSetReviewUserPrompt());
        if (config.getGptReviewCommitMessages()) {
            steps.add(DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES);
        }
        return steps;
    }

}
