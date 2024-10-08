package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.listener.IEventHandlerType;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistantHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventHandlerTypeChangeMerged implements IEventHandlerType {
    private final Configuration config;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final ICodeContextPolicy codeContextPolicy;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;

    EventHandlerTypeChangeMerged(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        this.config = config;
        this.changeSetData = changeSetData;
        this.change = change;
        this.codeContextPolicy = codeContextPolicy;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        log.debug("Initialized EventHandlerTypeChangeMerged for change ID: {}", change.getFullChangeId());
    }

    @Override
    public PreprocessResult preprocessEvent() {
        log.debug("Preprocessing event for change merged: {}", change.getFullChangeId());
        return PreprocessResult.OK;
    }

    @Override
    public void processEvent() {
        log.debug("Starting processing event for change merged: {}", change.getFullChangeId());
        ChatGptAssistantHandler chatGptAssistantHandler = new ChatGptAssistantHandler(
                config,
                changeSetData,
                change,
                codeContextPolicy,
                pluginDataHandlerProvider
        );
        chatGptAssistantHandler.flushAssistantAndVectorIds();
        log.debug("Flushed assistant and Vector Store IDs for change merged: {}", change.getFullChangeId());
    }
}
