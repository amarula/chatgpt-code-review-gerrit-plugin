package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;

@Singleton
@Slf4j
public class EventHandlerExecutor {
    private final Injector injector;
    private final ScheduledExecutorService executor;

    @Inject
    EventHandlerExecutor(
            Injector injector,
            WorkQueue workQueue,
            @PluginName String pluginName,
            PluginConfigFactory pluginConfigFactory
    ) {
        this.injector = injector;
        int maximumPoolSize = pluginConfigFactory.getFromGerritConfig(pluginName)
                .getInt("maximumPoolSize", 2);
        this.executor = workQueue.createQueue(maximumPoolSize, "ChatGPT request executor");
        log.debug("EventHandlerExecutor initialized with maximum pool size: {}", maximumPoolSize);
    }

    public void execute(Configuration config, Event event) {
        log.debug("Executing event handler for event: {}", event);
        GerritEventContextModule contextModule = new GerritEventContextModule(config, event);
        EventHandlerTask task = injector.createChildInjector(contextModule)
                .getInstance(EventHandlerTask.class);
        executor.execute(task);
        log.debug("Task submitted to executor for event: {}", event);
    }
}
