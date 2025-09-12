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

import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAITool;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIToolChoice;
import com.googlesource.gerrit.plugins.reviewai.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.jsonToClass;

@Slf4j
public class OpenAITools {
  public enum Functions {
    formatReplies,
    getContext
  }

  private static final String FILENAME_TOOL_FORMAT = "config/%sTool.json";
  private static final String FILENAME_TOOL_CHOICE_FORMAT = "config/%sToolChoice.json";

  private final String functionName;

  public OpenAITools(Functions function) {
    functionName = function.name();
  }

  public OpenAITool retrieveFunctionTool() {
    OpenAITool tools;
    try (InputStreamReader reader =
        FileUtils.getInputStreamReader(String.format(FILENAME_TOOL_FORMAT, functionName))) {
      tools = jsonToClass(reader, OpenAITool.class);
      log.debug("Successfully loaded format replies tool from JSON.");
    } catch (IOException e) {
      throw new RuntimeException("Failed to load data for OpenAI `" + functionName + "` tool", e);
    }
    return tools;
  }

  public OpenAIToolChoice retrieveFunctionToolChoice() {
    OpenAIToolChoice toolChoice;
    try (InputStreamReader reader =
        FileUtils.getInputStreamReader(String.format(FILENAME_TOOL_CHOICE_FORMAT, functionName))) {
      toolChoice = jsonToClass(reader, OpenAIToolChoice.class);
      log.debug("Successfully loaded format replies tool choice from JSON.");
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to load data for OpenAI `" + functionName + "` tool choice", e);
    }
    return toolChoice;
  }
}
