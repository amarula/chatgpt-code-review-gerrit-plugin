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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDataDump;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksPromptingParamInstructions;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksPromptingParamPrompts;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithDoubleNewLine;

@Slf4j
public class ClientCommandShowExecutor extends ClientCommandBase {
  private final ChangeSetData changeSetData;
  private final GerritChange change;
  private final ICodeContextPolicy codeContextPolicy;
  private final Localizer localizer;
  private final PluginDataHandlerProvider pluginDataHandlerProvider;
  private final List<String> itemsToShow = new ArrayList<>();

  public ClientCommandShowExecutor(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      PluginDataHandlerProvider pluginDataHandlerProvider,
      Localizer localizer) {
    super(config);
    this.localizer = localizer;
    this.change = change;
    this.changeSetData = changeSetData;
    this.codeContextPolicy = codeContextPolicy;
    this.pluginDataHandlerProvider = pluginDataHandlerProvider;
    log.debug("ClientShowCommandExecutor initialized.");
  }

  public void executeShowCommand(Map<BaseOptionSet, String> baseOptions) {
    log.debug("Executing Show Command: {}", baseOptions);
    for (BaseOptionSet baseOption : baseOptions.keySet()) {
      switch (baseOption) {
        case CONFIG -> commandDumpConfig();
        case LOCAL_DATA -> commandDumpStoredData();
        case PROMPTS -> commandShowPrompts();
        case INSTRUCTIONS -> commandShowInstructions();
      }
    }
    changeSetData.setReviewSystemMessage(joinWithDoubleNewLine(itemsToShow));
  }

  private void commandDumpConfig() {
    DebugCodeBlocksConfiguration debugCodeBlocksConfiguration =
        new DebugCodeBlocksConfiguration(localizer);
    itemsToShow.add(debugCodeBlocksConfiguration.getDebugCodeBlock(config));
  }

  private void commandDumpStoredData() {
    DebugCodeBlocksDataDump debugCodeBlocksDataDump =
        new DebugCodeBlocksDataDump(localizer, pluginDataHandlerProvider);
    itemsToShow.add(debugCodeBlocksDataDump.getDebugCodeBlock());
  }

  private void commandShowPrompts() {
    DebugCodeBlocksPromptingParamPrompts debugCodeBlocksPromptingParamPrompts =
        new DebugCodeBlocksPromptingParamPrompts(
            localizer, config, changeSetData, change, codeContextPolicy);
    itemsToShow.add(debugCodeBlocksPromptingParamPrompts.getDebugCodeBlock());
  }

  private void commandShowInstructions() {
    DebugCodeBlocksPromptingParamInstructions debugCodeBlocksPromptingParamInstructions =
        new DebugCodeBlocksPromptingParamInstructions(
            localizer, config, changeSetData, change, codeContextPolicy);
    itemsToShow.add(debugCodeBlocksPromptingParamInstructions.getDebugCodeBlock());
  }
}
