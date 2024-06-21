package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.events.Event;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataProvider;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptClientStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.chatgpt.ChatGptClientStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.gerrit.GerritClientPatchSetStateless;

import static com.google.inject.Scopes.SINGLETON;

public class GerritEventContextModule extends FactoryModule {
    private final Event event;
    private final Configuration config;

    public GerritEventContextModule(Configuration config, Event event) {
        this.event = event;
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(IChatGptClient.class).to(getChatGptMode());
        bind(IGerritClientPatchSet.class).to(getClientPatchSet());

        bind(Configuration.class).toInstance(config);
        bind(GerritChange.class).toInstance(new GerritChange(event));
        bind(ChangeSetData.class).toProvider(ChangeSetDataProvider.class).in(SINGLETON);
        bind(PluginDataHandler.class).toProvider(PluginDataHandlerProvider.class).in(Singleton.class);
    }

    private Class<? extends IChatGptClient> getChatGptMode() {
        return switch (config.getGptMode()){
            case stateful -> ChatGptClientStateful.class;
            case stateless -> ChatGptClientStateless.class;
        };
    }

    private Class<? extends IGerritClientPatchSet> getClientPatchSet() {
        return switch (config.getGptMode()){
            case stateful -> GerritClientPatchSetStateful.class;
            case stateless -> GerritClientPatchSetStateless.class;
        };
    }
}
