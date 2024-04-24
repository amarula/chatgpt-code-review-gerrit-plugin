package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.events.Event;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;

public class GerritEventContextModule extends FactoryModule {
    private final Event event;
    private final Configuration config;

    public GerritEventContextModule(Configuration config, Event event) {
        this.event = event;
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(config);
        bind(GerritChange.class).toInstance(new GerritChange(event));
    }
}
