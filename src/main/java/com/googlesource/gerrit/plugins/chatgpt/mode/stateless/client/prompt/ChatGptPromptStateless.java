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
                DEFAULT_GPT_SYSTEM_PROMPT_INSTRUCTIONS + DOT,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW
        )));
    }

    public static String getCommentRequestPrompt(int commentPropertiesSize) {
        log.debug("Constructing Stateless comment request prompt for {} comment properties.", commentPropertiesSize);
        return joinWithSpace(new ArrayList<>(List.of(
                DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT,
                buildFieldSpecifications(REQUEST_REPLY_ATTRIBUTES),
                DEFAULT_GPT_REPLIES_PROMPT_INLINE,
                String.format(DEFAULT_GPT_REPLIES_PROMPT_ENFORCE_RESPONSE_CHECK, commentPropertiesSize)
        )));
    }

    public String getGptSystemPrompt() {
        List<String> prompt = new ArrayList<>(Arrays.asList(
                config.getGptSystemPromptInstructions(DEFAULT_GPT_SYSTEM_PROMPT_INSTRUCTIONS) + DOT,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION
        ));
        if (!isCommentEvent) {
            prompt.add(DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW);
        }
        log.debug("Generated GPT System Prompt: {}", String.join(", ", prompt));
        return joinWithSpace(prompt);
    }

    public String getGptUserPrompt(ChangeSetData changeSetData, String patchSet) {
        List<String> prompt = new ArrayList<>();
        String gptRequestDataPrompt = changeSetData.getGptDataPrompt();
        boolean isValidRequestDataPrompt = gptRequestDataPrompt != null && !gptRequestDataPrompt.isEmpty();
        if (isCommentEvent && isValidRequestDataPrompt) {
            log.debug("Using request-specific data prompt for comments: {}", gptRequestDataPrompt);
            prompt.addAll(Arrays.asList(
                    DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                    patchSet,
                    DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                    gptRequestDataPrompt,
                    getCommentRequestPrompt(changeSetData.getCommentPropertiesSize())
            ));
        }
        else {
            log.debug("Using review-specific prompts for patch set.");
            prompt.add(DEFAULT_GPT_REVIEW_PROMPT);
            prompt.addAll(getReviewSteps());
            prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIFF);
            prompt.add(patchSet);
            if (isValidRequestDataPrompt) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY);
                prompt.add(gptRequestDataPrompt);
            }
            if (!config.getDirective().isEmpty()) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES);
                prompt.add(getNumberedListString(config.getDirective(), null, null));
            }
        }
        String userPrompt = joinWithNewLine(prompt);
        log.debug("Generated GPT User Prompt: {}", userPrompt);
        return userPrompt;
    }

    private void loadStatelessPrompts() {
        loadDefaultPrompts("promptsStateless");
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
            log.debug("Added commit message review prompts to the review steps.");
        }
        log.debug("Complete list of review steps prepared: {}", steps);
        return steps;
    }
}
