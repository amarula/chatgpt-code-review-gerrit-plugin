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

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.messages.ClientMessageCleaner;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AIComment extends ClientBase {
  protected ClientMessageCleaner messageCleaner;

  private final ChangeSetData changeSetData;
  private final Localizer localizer;

  public AIComment(Configuration config, ChangeSetData changeSetData, Localizer localizer) {
    super(config);
    this.changeSetData = changeSetData;
    this.localizer = localizer;
    log.debug("AIComment initialized");
  }

  public String getCleanedMessage(GerritComment commentProperty) {
    log.debug("Cleaning message for comment property: {}", commentProperty);
    messageCleaner = new ClientMessageCleaner(config, commentProperty.getMessage(), localizer);
    if (isFromAssistant(commentProperty)) {
      log.debug("Comment from assistant detected. Removing debug code blocks.");
      messageCleaner.removeDebugCodeBlocks();
    } else {
      log.debug("Comment not from assistant. Removing mentions and commands.");
      messageCleaner.removeMentions().removeCommands();
    }
    String cleanedMessage = messageCleaner.removeHeadings().getMessage();
    log.debug("Cleaned message: {}", cleanedMessage);
    return cleanedMessage;
  }

  protected boolean isFromAssistant(GerritComment commentProperty) {
    boolean fromAssistant =
        commentProperty.getAuthor().getAccountId() == changeSetData.getGptAccountId();
    log.debug("Checking if comment is from assistant: {}", fromAssistant);
    return fromAssistant;
  }
}
