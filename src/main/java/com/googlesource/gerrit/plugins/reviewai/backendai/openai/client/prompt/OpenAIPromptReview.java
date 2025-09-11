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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.googlesource.gerrit.plugins.reviewai.utils.TextUtils.*;

@Slf4j
public class OpenAIPromptReview extends OpenAIPromptBase implements IOpenAIPrompt {
  private static final String RULE_NUMBER_PREFIX = "RULE #";

  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_TASKS;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_GUIDELINES;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_DONT_GUESS_CODE;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_ON_DEMAND_REQUEST;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY;
  public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FOCUS_PATCH_SET;

  private final ICodeContextPolicy codeContextPolicy;

  public OpenAIPromptReview(
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy) {
    super(config, changeSetData, change, codeContextPolicy);
    this.codeContextPolicy = codeContextPolicy;
    loadDefaultPrompts("promptsOpenAIReview");
    log.debug("OpenAIPromptReview initialized for change ID: {}", change.getFullChangeId());
  }

  @Override
  public void addGptAssistantInstructions(List<String> instructions) {
    addReviewInstructions(instructions);
    if (config.getGptReviewCommitMessages()) {
      instructions.add(getReviewPromptCommitMessages());
    }
    log.debug("GPT Assistant Review Instructions added: {}", instructions);
  }

  @Override
  public String getGptRequestDataPrompt() {
    log.debug("No specific request data prompt for reviews.");
    return null;
  }

  protected void addReviewInstructions(List<String> instructions) {
    instructions.addAll(
        List.of(
            DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_TASKS,
            joinWithNewLine(
                new ArrayList<>(
                    List.of(
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_RULES,
                        getGptAssistantInstructionsReview(),
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_REVIEW_GUIDELINES,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT,
                        DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES))),
            getPatchSetReviewPrompt()));
    log.debug("Review instructions formed: {}", instructions);
  }

  protected String getGptAssistantInstructionsReview(boolean... ruleFilter) {
    // Rules are applied by default unless the corresponding ruleFilter values is set to false
    List<String> rules = new ArrayList<>();
    codeContextPolicy.addCodeContextPolicyAwareAssistantRule(rules);
    rules.addAll(
        List.of(
            DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_HISTORY,
            DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FOCUS_PATCH_SET));
    if (config.getDirective() != null) {
      rules.addAll(config.getDirective());
    }
    log.debug("Rules used in the assistant: {}", rules);
    return joinWithNewLine(
        getNumberedList(
            IntStream.range(0, rules.size())
                .filter(i -> i >= ruleFilter.length || ruleFilter[i])
                .mapToObj(rules::get)
                .collect(Collectors.toList()),
            RULE_NUMBER_PREFIX,
            COLON_SPACE));
  }
}
