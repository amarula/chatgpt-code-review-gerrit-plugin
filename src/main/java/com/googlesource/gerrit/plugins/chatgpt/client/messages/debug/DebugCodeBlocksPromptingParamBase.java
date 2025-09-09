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

package com.googlesource.gerrit.plugins.chatgpt.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptClient.ReviewAssistantStages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.client.prompt.ChatGptPromptFactory.getChatGptPrompt;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.distanceCodeDelimiter;

public abstract class DebugCodeBlocksPromptingParamBase extends DebugCodeBlocksComposer {
  private final Configuration config;
  private final ChangeSetData changeSetData;
  private final GerritChange change;
  private final ICodeContextPolicy codeContextPolicy;
  protected final LinkedHashMap<String, String> promptingParameters = new LinkedHashMap<>();

  protected IChatGptPrompt chatGptPrompt;

  public DebugCodeBlocksPromptingParamBase(
      Localizer localizer,
      String titleKey,
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(localizer, titleKey);
    this.config = config;
    this.change = change;
    this.changeSetData = changeSetData;
    this.codeContextPolicy = codeContextPolicy;
  }

  public String getDebugCodeBlock() {
    return super.getDebugCodeBlock(getPromptingParameters());
  }

  private List<String> getPromptingParameters() {
    populateParameters();
    return promptingParameters.entrySet().stream()
        .map(e -> getAsTitle(e.getKey()) + "\n" + distanceCodeDelimiter(e.getValue()) + "\n")
        .collect(Collectors.toList());
  }

  protected void populateParameters() {
    ChatGptParameters chatGptParameters = new ChatGptParameters(config, false);
    chatGptPrompt =
        getChatGptPrompt(
            config, changeSetData, change, codeContextPolicy, ReviewAssistantStages.REVIEW_CODE);
    if (chatGptParameters.shouldSpecializeAssistants()) {
      populateSpecializedCodeReviewParameters();
      chatGptPrompt =
          getChatGptPrompt(
              config,
              changeSetData,
              change,
              codeContextPolicy,
              ReviewAssistantStages.REVIEW_COMMIT_MESSAGE);
      populateSpecializedCommitMessageReviewParameters();
    } else {
      populateReviewParameters();
    }
  }

  protected abstract void populateSpecializedCodeReviewParameters();

  protected abstract void populateSpecializedCommitMessageReviewParameters();

  protected abstract void populateReviewParameters();
}
