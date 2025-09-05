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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands.ClientCommandParser;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

@Slf4j
public class ClientMessageParser extends ClientMessageBase {
  private final ClientCommandParser clientCommandParser;

  public ClientMessageParser(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      GitRepoFiles gitRepoFiles,
      PluginDataHandlerProvider pluginDataHandlerProvider,
      Localizer localizer) {
    super(config);
    clientCommandParser =
        new ClientCommandParser(
            config,
            changeSetData,
            change,
            codeContextPolicy,
            gitRepoFiles,
            pluginDataHandlerProvider,
            localizer);
    log.debug("ClientMessageParser initialized with bot mention pattern: {}", botMentionPattern);
  }

  public boolean isBotAddressed(String message) {
    log.debug("Checking if message addresses the bot: {}", message);
    Matcher userMatcher = botMentionPattern.matcher(message);
    if (!userMatcher.find()) {
      log.debug(
          "Skipping action since the comment does not mention the ChatGPT bot."
              + " Expected bot name in comment: {}, Actual comment text: {}",
          config.getGerritUserName(),
          message);
      return false;
    }
    return true;
  }

  public boolean parseCommands(String comment) {
    log.debug("Parsing commands from comment: {}", comment);
    return clientCommandParser.parseCommands(comment);
  }
}
