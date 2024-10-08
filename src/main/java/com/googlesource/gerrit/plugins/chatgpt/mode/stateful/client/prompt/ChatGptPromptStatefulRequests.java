package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithSpace;

@Slf4j
public class ChatGptPromptStatefulRequests extends ChatGptPromptStatefulBase implements IChatGptPromptStateful {
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REQUESTS;

    public ChatGptPromptStatefulRequests(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy
    ) {
        super(config, changeSetData, change, codeContextPolicy);
        loadDefaultPrompts("promptsStatefulRequests");
        log.debug("ChatGptPromptStatefulRequests initialized for change ID: {}", change.getFullChangeId());
    }

    @Override
    public void addGptAssistantInstructions(List<String> instructions) {
        instructions.addAll(List.of(
               DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REQUESTS,
               getCommentRequestPrompt(changeSetData.getCommentPropertiesSize())
        ));
        log.debug("GPT Assistant Instructions for requests added: {}", instructions);
    }

    @Override
    public String getGptRequestDataPrompt() {
        if (changeSetData == null) {
            log.warn("ChangeSetData is null, returning no prompt");
            return null;
        }
        String requestDataPrompt = changeSetData.getGptDataPrompt();
        log.debug("GPT Request Data Prompt retrieved: {}", requestDataPrompt);
        return requestDataPrompt;
    }

    private String getCommentRequestPrompt(int commentPropertiesSize) {
        log.debug("Constructing Stateful comment request prompt for {} comment properties.", commentPropertiesSize);
        return joinWithSpace(new ArrayList<>(List.of(
                buildFieldSpecifications(REQUEST_REPLY_ATTRIBUTES),
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
                DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES,
                DEFAULT_GPT_REPLIES_PROMPT_INLINE,
                String.format(DEFAULT_GPT_REPLIES_PROMPT_ENFORCE_RESPONSE_CHECK, commentPropertiesSize)
        )));
    }
}
