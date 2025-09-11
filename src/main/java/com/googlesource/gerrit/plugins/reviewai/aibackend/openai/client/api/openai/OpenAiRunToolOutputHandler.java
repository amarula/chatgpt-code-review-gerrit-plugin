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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiGetContextContent;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiToolCall;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAiRun;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.ondemand.CodeContextBuilder;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiToolOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAiRunToolOutputHandler extends OpenAiClientBase {
  // OpenAI may occasionally return the fixed string "multi_tool_use" as the function name when
  // multiple tools are
  // utilized.
  private static final List<String> ON_DEMAND_FUNCTION_NAMES =
      List.of("get_context", "multi_tool_use");

  private final OpenAiRun openAiRun;
  private final CodeContextBuilder codeContextBuilder;

  private List<OpenAiToolCall> openAiToolCalls;

  public OpenAiRunToolOutputHandler(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles, OpenAiRun openAiRun) {
    super(config);
    this.openAiRun = openAiRun;
    codeContextBuilder = new CodeContextBuilder(config, change, gitRepoFiles);
  }

  public void submitToolOutput(List<OpenAiToolCall> openAiToolCalls)
      throws OpenAiConnectionFailException {
    this.openAiToolCalls = openAiToolCalls;
    List<OpenAiToolOutput> toolOutputs = new ArrayList<>();
    log.debug("OpenAI Tool Calls: {}", openAiToolCalls);
    for (int i = 0; i < openAiToolCalls.size(); i++) {
      toolOutputs.add(
          OpenAiToolOutput.builder()
              .toolCallId(openAiToolCalls.get(i).getId())
              .output(getOutput(i))
              .build());
    }
    log.debug("OpenAI Tool Outputs: {}", toolOutputs);
    openAiRun.submitToolOutputs(toolOutputs);
  }

  private String getOutput(int i) {
    OpenAiToolCall.Function function = getFunction(openAiToolCalls, i);
    if (ON_DEMAND_FUNCTION_NAMES.contains(function.getName())) {
      OpenAiGetContextContent getContextContent =
          getArgumentAsType(openAiToolCalls, i, OpenAiGetContextContent.class);
      log.debug("OpenAI `get_context` Response Content: {}", getContextContent);
      return codeContextBuilder.buildCodeContext(getContextContent);
    }
    return "";
  }
}
