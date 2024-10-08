package com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context;

import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OperationNotSupportedException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptRunResponse;

import java.util.List;

public interface ICodeContextPolicy {
    void setupRunAction(ChatGptRun chatGptRun);
    boolean runActionRequired(ChatGptRunResponse runResponse) throws OpenAiConnectionFailException;
    String generateVectorStore() throws OpenAiConnectionFailException;
    void removeVectorStore() throws OperationNotSupportedException;
    void updateAssistantTools(ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId);
    void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions);
    void addCodeContextPolicyAwareAssistantRule(List<String> rules);
}
