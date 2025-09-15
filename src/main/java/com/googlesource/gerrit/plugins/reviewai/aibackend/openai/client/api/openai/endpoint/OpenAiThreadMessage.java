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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint;

import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.prompt.IAiPrompt;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiRequestMessage;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiApiBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiResponse;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class OpenAiThreadMessage extends OpenAiApiBase {
  private final String threadId;

  private ChangeSetData changeSetData;
  private GerritChange change;
  private ICodeContextPolicy codeContextPolicy;
  private String patchSet;
  private OpenAiRequestMessage addMessageRequestBody;

  public OpenAiThreadMessage(String threadId, Configuration config) {
    super(config);
    this.threadId = threadId;
  }

  public OpenAiThreadMessage(
      String threadId,
      Configuration config,
      ChangeSetData changeSetData,
      GerritChange change,
      ICodeContextPolicy codeContextPolicy,
      String patchSet) {
    this(threadId, config);
    this.changeSetData = changeSetData;
    this.change = change;
    this.codeContextPolicy = codeContextPolicy;
    this.patchSet = patchSet;
  }

  public OpenAiThreadMessageResponse retrieveMessage(String messageId)
      throws OpenAiConnectionFailException {
    Request request = createRetrieveMessageRequest(messageId);
    log.debug("OpenAI Retrieve Thread Message request: {}", request);
    OpenAiThreadMessageResponse threadMessageResponse =
        getOpenAiResponse(request, OpenAiThreadMessageResponse.class);
    log.info("Thread Message retrieved: {}", threadMessageResponse);

    return threadMessageResponse;
  }

  public void addMessage() throws OpenAiConnectionFailException {
    Request request = addMessageRequest();
    log.debug("OpenAI Add Message request: {}", request);

    OpenAiResponse addMessageResponse = getOpenAiResponse(request);
    log.info("Message added: {}", addMessageResponse);
  }

  public String getAddMessageRequestBody() {
    return getGson().toJson(addMessageRequestBody);
  }

  private Request createRetrieveMessageRequest(String messageId) {
    String uri = OpenAiUriResourceLocator.threadMessageRetrieveUri(threadId, messageId);
    log.debug("OpenAI Retrieve Thread Message request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, null);
  }

  private Request addMessageRequest() {
    String uri = OpenAiUriResourceLocator.threadMessagesUri(threadId);
    log.debug("OpenAI Add Message request URI: {}", uri);
    IAiPrompt openAiPromptOpenAI =
        AiPromptFactory.getAiPrompt(config, changeSetData, change, codeContextPolicy);
    addMessageRequestBody =
        OpenAiRequestMessage.builder()
            .role("user")
            .content(openAiPromptOpenAI.getDefaultAiThreadReviewMessage(patchSet))
            .build();
    log.debug("OpenAI Add Message request body: {}", addMessageRequestBody);

    return httpClient.createRequestFromJson(uri, addMessageRequestBody);
  }
}
