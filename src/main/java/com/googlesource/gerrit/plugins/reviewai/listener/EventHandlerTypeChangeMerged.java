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

package com.googlesource.gerrit.plugins.reviewai.listener;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OperationNotSupportedException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.listener.IEventHandlerType;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiAssistantHandler;
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
      PluginDataHandlerProvider pluginDataHandlerProvider) {
    this.config = config;
    this.changeSetData = changeSetData;
    this.change = change;
    this.codeContextPolicy = codeContextPolicy;
    this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    log.debug(
        "Initialized EventHandlerTypeChangeMerged for change ID: {}", change.getFullChangeId());
  }

  @Override
  public PreprocessResult preprocessEvent() {
    log.debug("Preprocessing event for change merged: {}", change.getFullChangeId());
    return PreprocessResult.OK;
  }

  @Override
  public void processEvent() {
    log.debug("Starting processing event for change merged: {}", change.getFullChangeId());
    OpenAiAssistantHandler openAiAssistantHandler =
        new OpenAiAssistantHandler(
            config, changeSetData, change, codeContextPolicy, pluginDataHandlerProvider);
    try {
      openAiAssistantHandler.flushAssistantAndVectorIds();
    } catch (OperationNotSupportedException e) {
      log.error("Exception while flushing assistant and vector ids", e);
      return;
    }
    log.debug(
        "Flushed assistant and Vector Store IDs for change merged: {}", change.getFullChangeId());
  }
}
