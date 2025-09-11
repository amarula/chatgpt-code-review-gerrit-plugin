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

package com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context;

import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OperationNotSupportedException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAiRun;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiAssistantTools;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiRunResponse;

import java.util.List;

public interface ICodeContextPolicy {
  void setupRunAction(OpenAiRun openAiRun);

  boolean runActionRequired(OpenAiRunResponse runResponse) throws OpenAiConnectionFailException;

  String generateVectorStore() throws OpenAiConnectionFailException;

  void removeVectorStore() throws OperationNotSupportedException;

  void updateAssistantTools(OpenAiAssistantTools openAIAssistantTools, String vectorStoreId);

  void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions);

  void addCodeContextPolicyAwareAssistantRule(List<String> rules);
}
