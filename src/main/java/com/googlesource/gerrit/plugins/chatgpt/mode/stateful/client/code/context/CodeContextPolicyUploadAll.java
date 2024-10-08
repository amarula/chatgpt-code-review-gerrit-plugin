package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStoreHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptAssistantTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptToolResources;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulBase.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulReview.DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;

@Slf4j
public class CodeContextPolicyUploadAll extends CodeContextPolicyBase implements ICodeContextPolicy {
    private final GerritChange change;
    private final ChatGptVectorStoreHandler chatGptVectorStoreHandler;

    @Getter
    private ChatGptToolResources toolResources;

    @VisibleForTesting
    @Inject
    public CodeContextPolicyUploadAll(
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.change = change;
        chatGptVectorStoreHandler = new ChatGptVectorStoreHandler(
                config,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider.getProjectScope()
        );
    }

    @Override
    public String generateVectorStore() throws OpenAiConnectionFailException {
        return chatGptVectorStoreHandler.generateVectorStore();
    }

    @Override
    public void removeVectorStore() {
        chatGptVectorStoreHandler.removeVectorStoreId();
    }

    @Override
    public void updateAssistantTools(ChatGptAssistantTools chatGptAssistantTools, String vectorStoreId) {
        chatGptAssistantTools.getTools().add(new ChatGptTool("file_search"));
        chatGptAssistantTools.setToolResources(
                new ChatGptToolResources(
                    new ChatGptToolResources.VectorStoreIds(
                            new String[]{vectorStoreId}
                    )
            )
        );
    }

    @Override
    public void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
        instructions.add(String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT, change.getProjectName()));
    }

    @Override
    public void addCodeContextPolicyAwareAssistantRule(List<String> rules) {
        rules.add(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE);
    }
}
