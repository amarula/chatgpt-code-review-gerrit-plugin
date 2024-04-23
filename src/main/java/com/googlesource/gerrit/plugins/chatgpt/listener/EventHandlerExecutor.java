package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class EventHandlerExecutor {
    private final ScheduledExecutorService executor;
    private final EventHandlerTask.Factory taskHandlerFactory;

    @Inject
    EventHandlerExecutor(
            WorkQueue workQueue,
            EventHandlerTask.Factory taskHandlerFactory,
            @PluginName String pluginName,
            PluginConfigFactory pluginConfigFactory
    ) {
        this.taskHandlerFactory = taskHandlerFactory;
        int maximumPoolSize = pluginConfigFactory.getFromGerritConfig(pluginName)
                .getInt("maximumPoolSize", 2);
        this.executor = workQueue.createQueue(maximumPoolSize, "ChatGPT request executor");
    }

    public void execute(Configuration config, Event event) {
        executor.execute(taskHandlerFactory.create(config, event));
    }

    void shutdown() {
        log.info("shutting down execution queue");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
