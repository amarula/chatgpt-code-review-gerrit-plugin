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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public abstract class ChatGptPromptStatefulBase extends ChatGptPrompt
    implements IChatGptPromptStateful {
  public static String DEFAULT_GPT_ASSISTANT_NAME;
  public static String DEFAULT_GPT_ASSISTANT_DESCRIPTION;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_NO_FILE_CONTEXT;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES;
  public static String DEFAULT_GPT_MESSAGE_REQUEST_RESEND_FORMATTED;
  public static String DEFAULT_GPT_MESSAGE_REVIEW;

  protected final ChangeSetData changeSetData;
  protected final GerritChange change;

  private final ICodeContextPolicy codeContextPolicy;

  public ChatGptPromptStatefulBase(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config);
    this.changeSetData = changeSetData;
    this.change = change;
    this.codeContextPolicy = codeContextPolicy;
    this.isCommentEvent = change.getIsCommentEvent();
    loadDefaultPrompts("promptsStateful");
    log.debug("Initialized ChatGptPromptStatefulBase with change ID: {}", change.getFullChangeId());
  }

  public String getDefaultGptAssistantDescription() {
    String description = String.format(DEFAULT_GPT_ASSISTANT_DESCRIPTION, change.getProjectName());
    log.debug("Generated GPT Assistant Description: {}", description);
    return description;
  }

  public abstract void addGptAssistantInstructions(List<String> instructions);

  public abstract String getGptRequestDataPrompt();

  public String getDefaultGptAssistantInstructions() {
    List<String> instructions =
        new ArrayList<>(
            List.of(
                config.getGptSystemPromptInstructions(DEFAULT_GPT_SYSTEM_PROMPT_INSTRUCTIONS)
                    + DOT));
    codeContextPolicy.addCodeContextPolicyAwareAssistantInstructions(instructions);
    addGptAssistantInstructions(instructions);
    String compiledInstructions = joinWithSpace(instructions);
    log.debug("Compiled GPT Assistant Instructions: {}", compiledInstructions);
    return compiledInstructions;
  }

  public String getDefaultGptThreadReviewMessage(String patchSet) {
    String gptRequestDataPrompt = getGptRequestDataPrompt();
    if (gptRequestDataPrompt != null && !gptRequestDataPrompt.isEmpty()) {
      log.debug("Request User Prompt retrieved: {}", gptRequestDataPrompt);
      return gptRequestDataPrompt;
    } else {
      String defaultMessage = String.format(DEFAULT_GPT_MESSAGE_REVIEW, patchSet);
      log.debug("Default Thread Review Message used: {}", defaultMessage);
      return defaultMessage;
    }
  }
}
