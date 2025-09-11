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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint;

import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.prompt.AIPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.backendai.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIRequestMessage;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIApiBase;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIResponse;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIThreadMessageResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class OpenAIThreadMessage extends OpenAIApiBase {
  private final String threadId;

  private ChangeSetData changeSetData;
  private GerritChange change;
  private ICodeContextPolicy codeContextPolicy;
  private String patchSet;
  private OpenAIRequestMessage addMessageRequestBody;

  public OpenAIThreadMessage(String threadId, Configuration config) {
    super(config);
    this.threadId = threadId;
  }

  public OpenAIThreadMessage(
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

  public OpenAIThreadMessageResponse retrieveMessage(String messageId)
      throws OpenAiConnectionFailException {
    Request request = createRetrieveMessageRequest(messageId);
    log.debug("OpenAI Retrieve Thread Message request: {}", request);
    OpenAIThreadMessageResponse threadMessageResponse =
        getOpenAIResponse(request, OpenAIThreadMessageResponse.class);
    log.info("Thread Message retrieved: {}", threadMessageResponse);

    return threadMessageResponse;
  }

  public void addMessage() throws OpenAiConnectionFailException {
    Request request = addMessageRequest();
    log.debug("OpenAI Add Message request: {}", request);

    OpenAIResponse addMessageResponse = getOpenAIResponse(request);
    log.info("Message added: {}", addMessageResponse);
  }

  public String getAddMessageRequestBody() {
    return getGson().toJson(addMessageRequestBody);
  }

  private Request createRetrieveMessageRequest(String messageId) {
    String uri = OpenAIUriResourceLocator.threadMessageRetrieveUri(threadId, messageId);
    log.debug("OpenAI Retrieve Thread Message request URI: {}", uri);

    return httpClient.createRequestFromJson(uri, null);
  }

  private Request addMessageRequest() {
    String uri = OpenAIUriResourceLocator.threadMessagesUri(threadId);
    log.debug("OpenAI Add Message request URI: {}", uri);
    IOpenAIPrompt openAIPromptOpenAI =
        AIPromptFactory.getOpenAIPromptOpenAI(config, changeSetData, change, codeContextPolicy);
    addMessageRequestBody =
        OpenAIRequestMessage.builder()
            .role("user")
            .content(openAIPromptOpenAI.getDefaultGptThreadReviewMessage(patchSet))
            .build();
    log.debug("OpenAI Add Message request body: {}", addMessageRequestBody);

    return httpClient.createRequestFromJson(uri, addMessageRequestBody);
  }
}
