package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetHelper.filterPatchWithoutCommitMessage;

@Slf4j
public class ChatGptPromptStatefulReviewCode extends ChatGptPromptStatefulReview implements IChatGptPromptStateful {

    public ChatGptPromptStatefulReviewCode(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(config, changeSetData, change, codeContextPolicy);
        log.debug("ChatGptPromptStatefulReviewCode initialized for project: {}", change.getProjectName());
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        addReviewInstructions(instructions);
        log.debug("Review Code specific GPT Assistant Instructions added: {}", instructions);
    }

    @Override
    public String getDefaultGptThreadReviewMessage(String patchSet) {
        String filteredPatchSet = filterPatchWithoutCommitMessage(change, patchSet);
        log.debug("Filtered Patch Set for Review Code: {}", filteredPatchSet);
        return super.getDefaultGptThreadReviewMessage(filteredPatchSet);
    }
}
