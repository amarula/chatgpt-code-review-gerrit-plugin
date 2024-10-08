package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class CodeContextPolicyBase extends ClientBase implements ICodeContextPolicy {
    public enum CodeContextPolicies {
        NONE,
        ON_DEMAND,
        UPLOAD_ALL
    }

    public CodeContextPolicyBase(Configuration config) {
        super(config);
    }

    public void setupRunAction(ChatGptRun chatGptRun) {
    }

    public boolean runActionRequired(ChatGptRunResponse runResponse) throws OpenAiConnectionFailException {
        return false;
    }

    public String generateVectorStore() throws OpenAiConnectionFailException {
        return null;
    }

    public void removeVectorStore() {
    }

    public void updateAssistantTools(ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
    }

    public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
    }

    public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
    }
}
