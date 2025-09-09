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

package com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptRunResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.jsonToClass;

@Slf4j
public abstract class ChatGptApiBase extends ClientBase {
  protected final ChatGptHttpClient httpClient;

  protected String clientResponse;

  public ChatGptApiBase(Configuration config) {
    super(config);
    httpClient = new ChatGptHttpClient(config);
  }

  public <T> T getChatGptResponse(Request request, Class<T> clazz)
      throws OpenAiConnectionFailException {
    log.debug("ChatGPT Client request: {}", request);
    clientResponse = httpClient.execute(request);
    log.debug("ChatGPT Client response: {}", clientResponse);

    return jsonToClass(clientResponse, clazz);
  }

  public ChatGptRunResponse getChatGptResponse(Request request)
      throws OpenAiConnectionFailException {
    return getChatGptResponse(request, ChatGptRunResponse.class);
  }
}
