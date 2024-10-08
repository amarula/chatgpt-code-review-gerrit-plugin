package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptRunActionHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulReview.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_ON_DEMAND_REQUEST;

@Slf4j
public class CodeContextPolicyOnDemand extends CodeContextPolicyBase implements ICodeContextPolicy {
    private ChatGptRunActionHandler chatGptRunActionHandler;

    @VisibleForTesting
    @Inject
    public CodeContextPolicyOnDemand(Configuration config) {
        super(config);
    }

    public void setupRunAction(ChatGptRun chatGptRun) {
        chatGptRunActionHandler = new ChatGptRunActionHandler(config, chatGptRun);
    }

    @Override
    public boolean runActionRequired(ChatGptRunResponse runResponse) throws OpenAiConnectionFailException {
        return chatGptRunActionHandler.runActionRequired(runResponse);
    }

    @Override
    public void updateAssistantTools(ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
        ChatGptTools chatGptGetContextTools = new ChatGptTools(ChatGptTools.Functions.getContext);
        chatGptAssistantTools.getTools().add(chatGptGetContextTools.retrieveFunctionTool());
    }

    @Override
    public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
        rules.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_ON_DEMAND_REQUEST);
    }
}
