package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.listener.IEventHandlerType;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistantBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistantReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventHandlerTypeChangeMerged implements IEventHandlerType {
    private final Configuration config;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;

    EventHandlerTypeChangeMerged(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        this.config = config;
        this.changeSetData = changeSetData;
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    }

    @Override
    public PreprocessResult preprocessEvent() {
        return PreprocessResult.OK;
    }

    @Override
    public void processEvent() {
        ChatGptAssistantBase chatGptAssistant = new ChatGptAssistantReview(
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider
        );
        chatGptAssistant.flushAssistantIds();
        chatGptAssistant.createVectorStore();
    }
}
