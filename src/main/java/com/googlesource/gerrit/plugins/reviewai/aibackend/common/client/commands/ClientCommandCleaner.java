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

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.commands;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

@Slf4j
public class ClientCommandCleaner extends ClientCommandBase {
  public ClientCommandCleaner(Configuration config) {
    super(config);
  }

  public String removeCommands(String comment) {
    log.debug("Removing commands from comment: {}", comment);

    Matcher messageCommandMatcher = MESSAGE_COMMAND_PATTERN.matcher(comment);
    if (messageCommandMatcher.find()) {
      return messageCommandMatcher.replaceAll("$1");
    }
    Matcher directiveCommandMatcher = DIRECTIVE_COMMAND_PATTERN.matcher(comment);
    if (directiveCommandMatcher.find()) {
      return directiveCommandMatcher.replaceAll("");
    }
    Matcher commandMatcher = COMMAND_PATTERN.matcher(comment);
    return commandMatcher.replaceAll("");
  }
}
