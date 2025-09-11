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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class OpenAIRunResponse extends OpenAIResponse {
  @SerializedName("required_action")
  private RequiredAction requiredAction;

  @Data
  public static class RequiredAction {
    @SerializedName("submit_tool_outputs")
    private SubmitToolOutputs submitToolOutputs;

    private String type;

    @Data
    public static class SubmitToolOutputs {
      @SerializedName("tool_calls")
      private List<OpenAIToolCall> toolCalls;
    }
  }
}
