/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.listener;

import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.events.*;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerBaseProvider;
import com.googlesource.gerrit.plugins.chatgpt.logging.LoggingConfigurationDeployed;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.EVENT_CLASS_MAP;

@Slf4j
public class GerritListener implements EventListener {
    private final String myInstanceId;
    private final ConfigCreator configCreator;
    private final EventHandlerExecutor evenHandlerExecutor;
    private final PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider;

    @Inject
    public GerritListener(
            ConfigCreator configCreator,
            EventHandlerExecutor evenHandlerExecutor,
            PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider,
            @GerritInstanceId @Nullable String myInstanceId
    ) {
        this.configCreator = configCreator;
        this.evenHandlerExecutor = evenHandlerExecutor;
        this.pluginDataHandlerBaseProvider = pluginDataHandlerBaseProvider;
        this.myInstanceId = myInstanceId;
        log.debug("GerritListener initialized with instance ID: {}", myInstanceId);
    }

    @Override
    public void onEvent(Event event) {
        log.debug("Received event: {}", event.getType());
        if (!Objects.equals(event.instanceId, myInstanceId)) {
            log.debug("Ignore event from another instance: {}", event.instanceId);
            return;
        }
        if (!EVENT_CLASS_MAP.containsValue(event.getClass())) {
            log.debug("The event {} is not managed by the plugin", event.getType());
            return;
        }

        log.info("Processing event: {}", event);
        PatchSetEvent patchSetEvent = (PatchSetEvent) event;
        Project.NameKey projectNameKey = patchSetEvent.getProjectNameKey();
        Change.Key changeKey = patchSetEvent.getChangeKey();

        try {
            log.debug("Creating configuration for project: {} and change: {}", projectNameKey, changeKey);
            Configuration config = configCreator.createConfig(projectNameKey, changeKey);
            log.debug("Configuration created, configuring logging...");
            LoggingConfigurationDeployed.configure(config, pluginDataHandlerBaseProvider);
            log.debug("Configuration and logging set, executing event handler...");
            evenHandlerExecutor.execute(config, patchSetEvent);
        } catch (NoSuchProjectException e) {
            log.error("Project not found: {}", projectNameKey, e);
        }
    }
}
