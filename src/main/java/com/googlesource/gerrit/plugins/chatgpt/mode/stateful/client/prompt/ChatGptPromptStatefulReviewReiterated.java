package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

@Slf4j
public class ChatGptPromptStatefulReviewReiterated extends ChatGptPromptStatefulReview implements IChatGptPromptStateful {

    public ChatGptPromptStatefulReviewReiterated(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(config, changeSetData, change, codeContextPolicy);
        log.debug("ChatGptPromptStatefulReviewReiterated initialized for project: {}", change.getProjectName());
    }

    @Override
    public String getDefaultGptThreadReviewMessage(String patchSet) {
        return joinWithNewLine(List.of(
                DEFAULT_GPT_MESSAGE_REQUEST_RESEND_FORMATTED,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES
        ));
    }
}
