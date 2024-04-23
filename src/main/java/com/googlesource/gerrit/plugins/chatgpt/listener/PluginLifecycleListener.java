package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PluginLifecycleListener implements LifecycleListener {
    public static final LifecycleModule MODULE = new LifecycleModule() {
        @Override
        protected void configure() {
            listener().to(PluginLifecycleListener.class);
        }
    };

    private final Thread shutdownHook;
    private final EventHandlerExecutor executor;

    @Inject
    PluginLifecycleListener(EventHandlerExecutor executor) {
        this.shutdownHook = new Thread(executor::shutdown);
        this.executor = executor;
    }

    @Override
    public void start() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public void stop() {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        executor.shutdown();
    }
}
