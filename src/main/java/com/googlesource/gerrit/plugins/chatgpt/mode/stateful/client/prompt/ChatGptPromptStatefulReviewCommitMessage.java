package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetHelper.filterCommitMessage;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

@Slf4j
public class ChatGptPromptStatefulReviewCommitMessage extends ChatGptPromptStatefulReview implements IChatGptPromptStateful {
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES_GUIDELINES;

    public ChatGptPromptStatefulReviewCommitMessage(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config, changeSetData, change);
        loadDefaultPrompts("promptsStatefulReviewCommitMessage");
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        instructions.addAll(List.of(
                joinWithNewLine(new ArrayList<>(List.of(
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES,
                        getGptAssistantInstructionsReview(true, false, true),
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES
                ))),
                DEFAULT_GPT_REVIEW_PROMPT_INSTRUCTIONS_COMMIT_MESSAGES,
                getPatchSetReviewPromptInstructions(),
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_COMMIT_MESSAGES_GUIDELINES
        ));
    }

    @Override
    public String getDefaultGptThreadReviewMessage(String patchSet) {
        return super.getDefaultGptThreadReviewMessage(filterCommitMessage(patchSet));
    }
}
