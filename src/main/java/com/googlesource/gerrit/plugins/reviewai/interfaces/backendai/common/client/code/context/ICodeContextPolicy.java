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

package com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context;

import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OperationNotSupportedException;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint.OpenAIRun;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIAssistantTools;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIRunResponse;

import java.util.List;

public interface ICodeContextPolicy {
  void setupRunAction(OpenAIRun openAIRun);

  boolean runActionRequired(OpenAIRunResponse runResponse) throws OpenAiConnectionFailException;

  String generateVectorStore() throws OpenAiConnectionFailException;

  void removeVectorStore() throws OperationNotSupportedException;

  void updateAssistantTools(OpenAIAssistantTools openAIAssistantTools, String vectorStoreId);

  void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions);

  void addCodeContextPolicyAwareAssistantRule(List<String> rules);
}
