package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gerrit.server.events.Event;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class EventHandlerExecutor {
    private final Injector injector;
    private final ExecutorService executor;

    @Inject
    EventHandlerExecutor(Injector injector) {
        this.injector = injector;
        this.executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("ChatGPT request executor").build());
    }

    public void execute(Configuration config, Event event) {
        GerritEventContextModule contextModule = new GerritEventContextModule(config);
        EventHandlerTask.Factory taskHandlerFactory = injector.createChildInjector(contextModule)
                .getInstance(EventHandlerTask.Factory.class);
        executor.execute(taskHandlerFactory.create(event));
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
