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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIGetContextContent;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIToolCall;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint.OpenAIRun;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.code.context.ondemand.CodeContextBuilder;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIToolOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OpenAIRunToolOutputHandler extends OpenAIClientBase {
  // OpenAI may occasionally return the fixed string "multi_tool_use" as the function name when
  // multiple tools are
  // utilized.
  private static final List<String> ON_DEMAND_FUNCTION_NAMES =
      List.of("get_context", "multi_tool_use");

  private final OpenAIRun openAIRun;
  private final CodeContextBuilder codeContextBuilder;

  private List<OpenAIToolCall> openAIToolCalls;

  public OpenAIRunToolOutputHandler(
      Configuration config, GerritChange change, GitRepoFiles gitRepoFiles, OpenAIRun openAIRun) {
    super(config);
    this.openAIRun = openAIRun;
    codeContextBuilder = new CodeContextBuilder(config, change, gitRepoFiles);
  }

  public void submitToolOutput(List<OpenAIToolCall> openAIToolCalls)
      throws OpenAiConnectionFailException {
    this.openAIToolCalls = openAIToolCalls;
    List<OpenAIToolOutput> toolOutputs = new ArrayList<>();
    log.debug("OpenAI Tool Calls: {}", openAIToolCalls);
    for (int i = 0; i < openAIToolCalls.size(); i++) {
      toolOutputs.add(
          OpenAIToolOutput.builder()
              .toolCallId(openAIToolCalls.get(i).getId())
              .output(getOutput(i))
              .build());
    }
    log.debug("OpenAI Tool Outputs: {}", toolOutputs);
    openAIRun.submitToolOutputs(toolOutputs);
  }

  private String getOutput(int i) {
    OpenAIToolCall.Function function = getFunction(openAIToolCalls, i);
    if (ON_DEMAND_FUNCTION_NAMES.contains(function.getName())) {
      OpenAIGetContextContent getContextContent =
          getArgumentAsType(openAIToolCalls, i, OpenAIGetContextContent.class);
      log.debug("OpenAI `get_context` Response Content: {}", getContextContent);
      return codeContextBuilder.buildCodeContext(getContextContent);
    }
    return "";
  }
}
