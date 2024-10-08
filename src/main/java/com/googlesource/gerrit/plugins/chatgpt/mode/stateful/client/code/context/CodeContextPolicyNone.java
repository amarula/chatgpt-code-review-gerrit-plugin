package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulBase.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_NO_FILE_CONTEXT;

@Slf4j
public class CodeContextPolicyNone extends CodeContextPolicyBase implements ICodeContextPolicy {

    @VisibleForTesting
    @Inject
    public CodeContextPolicyNone(Configuration config) {
        super(config);
        log.debug("CodeContextPolicyNone initialized");
    }

    @Override
    public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
        instructions.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_NO_FILE_CONTEXT);
        log.debug("Added Assistant Instructions for the current code context policy");
    }
}
