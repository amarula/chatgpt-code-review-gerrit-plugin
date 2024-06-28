package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public class ChatGptPromptStatefulReview extends ChatGptPromptStatefulBase implements IChatGptPromptStateful {
    private static final String RULE_NUMBER_PREFIX = "RULE #";

    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY;

    public ChatGptPromptStatefulReview(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config, changeSetData, change);
        if (DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW == null) {
            loadDefaultPrompts("promptsStatefulReview");
        }
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        instructions.addAll(List.of(
                getGptAssistantInstructionsReview(),
                getPatchSetReviewPrompt()
        ));
        if (config.getGptReviewCommitMessages()) {
            instructions.add(getReviewPromptCommitMessages());
        }
    }

    @Override
    public String getGptRequestDataPrompt() {
        return null;
    }

    private String getGptAssistantInstructionsReview() {
        return String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW, joinWithNewLine(getNumberedList(
                new ArrayList<>(List.of(
                        DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY
                )),
                RULE_NUMBER_PREFIX, COLON_SPACE
        )));
    }
}
