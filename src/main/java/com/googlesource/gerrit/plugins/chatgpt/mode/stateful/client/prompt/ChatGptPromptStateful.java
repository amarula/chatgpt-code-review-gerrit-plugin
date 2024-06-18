package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;


@Slf4j
public class ChatGptPromptStateful extends ChatGptPrompt {
    private static final String RULE_NUMBER_PREFIX = "RULE #";

    public static String DEFAULT_GPT_ASSISTANT_NAME;
    public static String DEFAULT_GPT_ASSISTANT_DESCRIPTION;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REQUESTS;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY;
    public static String DEFAULT_GPT_MESSAGE_REVIEW;

    private final ChangeSetData changeSetData;
    private final GerritChange change;

    public ChatGptPromptStateful(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config);
        this.changeSetData = changeSetData;
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
        List<String> instructions = new ArrayList<>(List.of(
                DEFAULT_GPT_SYSTEM_PROMPT + DOT,
                String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS, change.getProjectName())
        ));
        if (change.getIsCommentEvent()) {
            instructions.addAll(List.of(
                    DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REQUESTS,
                    getCommentRequestUserPrompt(changeSetData.getCommentPropertiesSize())
            ));
        }
        else {
            instructions.addAll(List.of(
                    getGptAssistantInstructionsReview(),
                    getPatchSetReviewUserPrompt()
            ));
            if (config.getGptReviewCommitMessages()) {
                instructions.add(getReviewPromptCommitMessages());
            }
        }
        return String.join(SPACE, instructions);
    }

    public String getDefaultGptThreadReviewMessage(String patchSet) {
        String gptRequestUserPrompt = getGptRequestUserPrompt();
        if (gptRequestUserPrompt != null && !gptRequestUserPrompt.isEmpty()) {
            log.debug("Request User Prompt retrieved: {}", gptRequestUserPrompt);
            return gptRequestUserPrompt;
        }
        else {
            return String.format(DEFAULT_GPT_MESSAGE_REVIEW, patchSet);
        }
    }

    private String getGptRequestUserPrompt() {
        if (changeSetData == null || !isCommentEvent) return null;
        return changeSetData.getGptRequestUserPrompt();
    }

    private String getGptAssistantInstructionsReview() {
        return String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW, joinWithNewLine(getNumberedList(
                new ArrayList<>(List.of(
                        DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY
                )),
                RULE_NUMBER_PREFIX, COLON
        )));
    }
}
