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

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.ai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiResponseContent;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiToolCall;

import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.jsonToClass;

public abstract class AiClientBase extends ClientBase {

  public AiClientBase(Configuration config) {
    super(config);
  }

  protected AiResponseContent convertResponseContentFromJson(String content) {
    return jsonToClass(content, AiResponseContent.class);
  }

  protected AiToolCall.Function getFunction(List<AiToolCall> toolCalls, int ind) {
    return toolCalls.get(ind).getFunction();
  }

  protected String getArgumentAsString(List<AiToolCall> toolCalls, int ind) {
    return getFunction(toolCalls, ind).getArguments();
  }

  protected AiResponseContent getArgumentAsResponse(List<AiToolCall> toolCalls, int ind) {
    return convertResponseContentFromJson(getArgumentAsString(toolCalls, ind));
  }

  protected <T> T getArgumentAsType(List<AiToolCall> toolCalls, int ind, Class<T> clazz) {
    return jsonToClass(getArgumentAsString(toolCalls, ind), clazz);
  }
}
