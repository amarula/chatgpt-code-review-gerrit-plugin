package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.config.FactoryModule;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;

public class GerritEventContextModule extends FactoryModule {
    private final Configuration config;

    public GerritEventContextModule(Configuration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        install(EventHandlerTask.MODULE);

        bind(Configuration.class).toInstance(config);
    }
}
