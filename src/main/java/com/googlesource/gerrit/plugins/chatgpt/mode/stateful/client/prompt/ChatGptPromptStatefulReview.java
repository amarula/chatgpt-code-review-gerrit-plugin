package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptUploadPolicies;
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
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FOCUS_PATCH_SET;

    public ChatGptPromptStatefulReview(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config, changeSetData, change);
        loadDefaultPrompts("promptsStatefulReview");
        log.debug("ChatGptPromptStatefulReview initialized for change ID: {}", change.getFullChangeId());
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        addReviewInstructions(instructions);
        if (config.getGptReviewCommitMessages()) {
            instructions.add(getReviewPromptCommitMessages());
        }
        log.debug("GPT Assistant Review Instructions added: {}", instructions);
    }

    @Override
    public String getGptRequestDataPrompt() {
        log.debug("No specific request data prompt for reviews.");
        return null;
    }

    protected void addReviewInstructions(List<String> instructions) {
        instructions.addAll(List.of(
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_TASKS,
                joinWithNewLine(new ArrayList<>(List.of(
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES,
                        getGptAssistantInstructionsReview(),
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_GUIDELINES,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES
                ))),
                getPatchSetReviewPrompt()
        ));
        log.debug("Review instructions formed: {}", instructions);
    }

    protected String getGptAssistantInstructionsReview(boolean... ruleFilter) {
        // Rules are applied by default unless the corresponding ruleFilter values is set to false
        ArrayList<String> rules = new ArrayList<>(List.of(
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FOCUS_PATCH_SET
        ));
        if (config.getGitCodebaseUploadPolicy() != ChatGptUploadPolicies.UploadPolicies.NONE) {
            rules.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE);
        }
        if (config.getDirective() != null) {
            rules.addAll(config.getDirective());
        }
        log.debug("Rules used in the assistant: {}", rules);
        return joinWithNewLine(getNumberedList(
                IntStream.range(0, rules.size())
                        .filter(i -> i >= ruleFilter.length || ruleFilter[i])
                        .mapToObj(rules::get)
                        .collect(Collectors.toList()),
                RULE_NUMBER_PREFIX, COLON_SPACE
        ));
    }
}
