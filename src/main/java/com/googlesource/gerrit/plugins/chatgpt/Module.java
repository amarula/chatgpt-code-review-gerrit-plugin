package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask;
import com.googlesource.gerrit.plugins.chatgpt.listener.GerritListener;
import com.googlesource.gerrit.plugins.chatgpt.listener.PluginLifecycleListener;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(EventHandlerTask.MODULE);
        install(PluginLifecycleListener.MODULE);

        Multibinder<EventListener> eventListenerBinder = Multibinder.newSetBinder(binder(), EventListener.class);
        eventListenerBinder.addBinding().to(GerritListener.class);
    }
}
