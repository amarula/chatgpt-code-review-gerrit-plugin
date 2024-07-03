package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetHelper.filterPatchWithoutCommitMessage;

@Slf4j
public class ChatGptPromptStatefulReviewCode extends ChatGptPromptStatefulReview implements IChatGptPromptStateful {

    public ChatGptPromptStatefulReviewCode(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config, changeSetData, change);
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        addReviewInstructions(instructions);
    }

    @Override
    public String getDefaultGptThreadReviewMessage(String patchSet) {
        return super.getDefaultGptThreadReviewMessage(filterPatchWithoutCommitMessage(change, patchSet));
    }
}
