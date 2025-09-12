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
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.prompt.IAIDataPrompt;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIMessageItem;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class AIDataPrompt {
  private final IAIDataPrompt openAIDataPromptHandler;

  public AIDataPrompt(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      GerritClientData gerritClientData,
      Localizer localizer) {
    openAIDataPromptHandler =
        AIPromptFactory.getOpenAIDataPrompt(
            config, changeSetData, change, gerritClientData, localizer);
    log.debug("AIDataPrompt initialized for change: {}", change.getFullChangeId());
  }

  public String buildPrompt() {
    log.debug("Building data prompt for OpenAI.");
    for (int i = 0; i < openAIDataPromptHandler.getCommentProperties().size(); i++) {
      openAIDataPromptHandler.addMessageItem(i);
      log.debug("Added message item to prompt for comment index: {}", i);
    }
    List<OpenAIMessageItem> messageItems = openAIDataPromptHandler.getMessageItems();
    String promptJson = messageItems.isEmpty() ? "" : getGson().toJson(messageItems);
    log.debug("Final OpenAI prompt JSON: {}", promptJson);
    return promptJson;
  }
}
