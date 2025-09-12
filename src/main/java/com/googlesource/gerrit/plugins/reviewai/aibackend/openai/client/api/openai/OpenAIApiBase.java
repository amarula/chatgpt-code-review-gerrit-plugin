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
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIRunResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.jsonToClass;

@Slf4j
public abstract class OpenAIApiBase extends ClientBase {
  protected final OpenAIHttpClient httpClient;

  protected String clientResponse;

  public OpenAIApiBase(Configuration config) {
    super(config);
    httpClient = new OpenAIHttpClient(config);
  }

  public <T> T getOpenAIResponse(Request request, Class<T> clazz)
      throws OpenAiConnectionFailException {
    log.debug("OpenAI Client request: {}", request);
    clientResponse = httpClient.execute(request);
    log.debug("OpenAI Client response: {}", clientResponse);

    return jsonToClass(clientResponse, clazz);
  }

  public OpenAIRunResponse getOpenAIResponse(Request request) throws OpenAiConnectionFailException {
    return getOpenAIResponse(request, OpenAIRunResponse.class);
  }
}
