package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

@Slf4j
public class ChatGptPromptStatefulReview extends ChatGptPromptStatefulBase implements IChatGptPromptStateful {
    private static final String RULE_NUMBER_PREFIX = "RULE #";

    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_TASKS;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_GUIDELINES;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY;

    public ChatGptPromptStatefulReview(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config, changeSetData, change);
        loadDefaultPrompts("promptsStatefulReview");
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        addReviewInstructions(instructions);
        if (config.getGptReviewCommitMessages()) {
            instructions.add(getReviewPromptCommitMessages());
        }
    }

    @Override
    public String getGptRequestDataPrompt() {
        return null;
    }

    protected void addReviewInstructions(List<String> instructions) {
        instructions.addAll(List.of(
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_TASKS,
                joinWithNewLine(new ArrayList<>(List.of(
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES,
                        getGptAssistantInstructionsReview(),
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_GUIDELINES
                ))),
                getPatchSetReviewPrompt()
        ));
    }

    protected String getGptAssistantInstructionsReview(boolean... ruleFilter) {
        // Rules are applied by default unless the corresponding ruleFilter values is set to false
        ArrayList<String> rules = new ArrayList<>(List.of(
                DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY
        ));
        return joinWithNewLine(getNumberedList(
                IntStream.range(0, rules.size())
                        .filter(i -> i >= ruleFilter.length || ruleFilter[i])
                        .mapToObj(rules::get)
                        .collect(Collectors.toList()),
                RULE_NUMBER_PREFIX, COLON_SPACE
        ));
    }
}
