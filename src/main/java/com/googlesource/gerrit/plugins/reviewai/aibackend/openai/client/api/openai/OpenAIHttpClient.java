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
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.http.HttpClient;
import okhttp3.Request;

import java.util.Map;

public class OpenAIHttpClient extends HttpClient {
  private static final Map<String, String> BETA_VERSION_HEADER =
      Map.of("OpenAI-Beta", "assistants=v2");

  public OpenAIHttpClient(Configuration config) {
    super(config);
  }

  @Override
  public Request createRequestFromJson(String uri, Object requestObject) {
    return createRequestFromJson(uri, requestObject, BETA_VERSION_HEADER);
  }
}
