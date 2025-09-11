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

package com.googlesource.gerrit.plugins.reviewai.openai;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.prompt.OpenAIPromptReviewReiterated;
import com.googlesource.gerrit.plugins.reviewai.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.googlesource.gerrit.plugins.reviewai.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.OpenAIPoller.FAILED_STATUS;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class OpenAIReviewUnifiedTest extends OpenAIReviewTestBase {
  private static final String OPENAI_ASSISTANT_ID = "asst_TEST_ASSISTANT_ID";

  @Rule public TestName testName = new TestName();

  @Override
  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    setupMockRequestCreateAssistant(OPENAI_ASSISTANT_ID);
    setupMockRequestCreateRun(OPENAI_ASSISTANT_ID, OPENAI_RUN_ID);
    setupMockRequestRetrieveRunSteps("openAIRunStepsResponse.json");

    if ("vectorStoreCreateFailure".equals(testName.getMethodName())) {
      when(globalConfig.getInt(Mockito.eq("gptPollingTimeout"), Mockito.anyInt())).thenReturn(0);
    }
  }

  private void setupVectorStoreFailure() {
    // Mock the behavior of the OpenAI create-vector-store-file-batch request with failure
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.vectorStoreFileBatchCreateUri(OPENAI_VECTOR_STORE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(
                        "{\"id\": "
                            + OPENAI_VECTOR_STORE_FILE_BATCH_ID
                            + ", \"status\": "
                            + FAILED_STATUS
                            + "}")));
    when(globalConfig.getInt(Mockito.eq("gptPollingInterval"), Mockito.anyInt())).thenReturn(0);
  }

  @Test
  public void patchSetCreatedOrUpdated() throws Exception {
    String reviewMessageCode = getReviewMessage("__files/openai/openAIRunStepsResponse.json", 0);
    String reviewMessageCommitMessage =
        getReviewMessage("__files/openai/openAIRunStepsResponse.json", 1);

    String reviewPrompt = openAIPrompt.getDefaultGptThreadReviewMessage(formattedPatchContent);

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(reviewPrompt, requestContent);
    Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }

  @Test
  public void filesCreateResponse400() {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAIUriResourceLocator.filesCreateUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_BAD_REQUEST)
                    .withHeader(
                        HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));
    // This test executes 2 retries with the retry interval configured to 0.
    when(globalConfig.getInt(Mockito.eq("gptConnectionMaxRetryAttempts"), Mockito.anyInt()))
        .thenReturn(2);
    when(globalConfig.getInt(Mockito.eq("gptConnectionRetryInterval"), Mockito.anyInt()))
        .thenReturn(0);

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    Assert.assertEquals(
        localizer.getText("message.openai.connection.error"),
        changeSetData.getReviewSystemMessage());
  }

  @Test
  public void vectorStoreCreateFirstTimeFailure() throws Exception {
    String reviewMessageCode = getReviewMessage("__files/openai/openAIRunStepsResponse.json", 0);
    setupVectorStoreFailure();

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
  }

  @Test
  public void vectorStoreCreateFailure() {
    setupVectorStoreFailure();
    // Mock the behavior of the OpenAI retrieve-vector-store-file-batch request with failure
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.vectorStoreFileBatchRetrieveUri(
                        OPENAI_VECTOR_STORE_ID, OPENAI_VECTOR_STORE_FILE_BATCH_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"status\": " + FAILED_STATUS + "}")));

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    Assert.assertEquals(
        localizer.getText("message.openai.connection.error"),
        changeSetData.getReviewSystemMessage());
  }

  @Test
  public void threadCreateResponse400() {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAIUriResourceLocator.threadsUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_BAD_REQUEST)
                    .withHeader(
                        HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    Assert.assertEquals(
        localizer.getText("message.openai.connection.error"),
        changeSetData.getReviewSystemMessage());
  }

  @Test
  public void runCreateResponse400() {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAIUriResourceLocator.runsUri(OPENAI_THREAD_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_BAD_REQUEST)
                    .withHeader(
                        HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));
    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    Assert.assertEquals(
        localizer.getText("message.openai.connection.error"),
        changeSetData.getReviewSystemMessage());
  }

  @Test
  public void runStepsInitialEmptyResponse() throws Exception {
    // To effectively test how an initial empty response from OpenAI is managed, the following
    // approach is adopted:
    // 1. the OpenAI run-steps request is initially mocked to return an empty data field, and
    // 2. the sleep function is mocked to replace the empty response with a valid one, instead of
    // pausing execution
    setupMockRequestRetrieveRunSteps("openAIRunStepsEmptyResponse.json");

    try (MockedStatic<ThreadUtils> mocked = Mockito.mockStatic(ThreadUtils.class)) {
      mocked
          .when(() -> ThreadUtils.threadSleep(Mockito.anyLong()))
          .thenAnswer(
              invocation -> {
                setupMockRequestRetrieveRunSteps("openAIRunStepsResponse.json");
                return null;
              });

      String reviewMessageCode = getReviewMessage("__files/openai/openAIRunStepsResponse.json", 0);
      String reviewMessageCommitMessage =
          getReviewMessage("__files/openai/openAIRunStepsResponse.json", 1);

      String reviewPrompt = openAIPrompt.getDefaultGptThreadReviewMessage(formattedPatchContent);

      handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

      ArgumentCaptor<ReviewInput> captor = testRequestSent();
      Assert.assertEquals(reviewPrompt, requestContent);
      Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
      Assert.assertEquals(
          reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }
  }

  @Test
  public void runStepsResponse400() {
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.runStepsUri(OPENAI_THREAD_ID, OPENAI_RUN_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_BAD_REQUEST)
                    .withHeader(
                        HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));

    try (MockedStatic<ThreadUtils> mocked = Mockito.mockStatic(ThreadUtils.class)) {
      mocked.when(() -> ThreadUtils.threadSleep(Mockito.anyLong())).then(invocation -> null);

      handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

      Assert.assertEquals(
          localizer.getText("message.openai.connection.error"),
          changeSetData.getReviewSystemMessage());
    }
  }

  @Test
  public void patchSetCreatedReiterateRequestForTextualResponse() throws Exception {
    String reviewReiteratePrompt =
        new OpenAIPromptReviewReiterated(
                config, changeSetData, getGerritChange(), getCodeContextPolicy())
            .getDefaultGptThreadReviewMessage("");

    setupMockRequestRetrieveRunSteps("openAIResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.threadMessageRetrieveUri(
                        OPENAI_THREAD_ID, OPENAI_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("openai/openAIResponseThreadMessageText.json")));

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    testRequestSent();
    Assert.assertEquals(reviewReiteratePrompt, requestContent);
  }

  @Test
  public void patchSetCreatedReiterateRequestForMalformedJson() throws Exception {
    String reviewReiteratePrompt =
        new OpenAIPromptReviewReiterated(
                config, changeSetData, getGerritChange(), getCodeContextPolicy())
            .getDefaultGptThreadReviewMessage("");

    setupMockRequestRetrieveRunSteps("openAIRunStepsResponseMalformedJson.json");

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    testRequestSent();
    Assert.assertEquals(reviewReiteratePrompt, requestContent);
  }

  @Test
  public void gptMentionedInComment() throws RestApiException {
    String reviewMessageCommitMessage =
        getReviewMessage("__files/openai/openAIResponseRequest.json", 0);

    openAIPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("openAIResponseRequest.json");

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }

  @Test
  public void gptMentionedInCommentMessageResponseText() throws RestApiException {
    String reviewMessageCommitMessage =
        getReviewMessage("__files/openai/openAIResponseRequest.json", 0);

    openAIPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("openAIResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.threadMessageRetrieveUri(
                        OPENAI_THREAD_ID, OPENAI_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("openai/openAIResponseThreadMessageText.json")));

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }

  @Test
  public void gptMentionedInCommentMessageResponseText400() {
    openAIPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("openAIResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.threadMessageRetrieveUri(
                        OPENAI_THREAD_ID, OPENAI_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_BAD_REQUEST)
                    .withHeader(
                        HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    Assert.assertEquals(
        localizer.getText("message.openai.connection.error"),
        changeSetData.getReviewSystemMessage());
  }

  @Test
  public void gptMentionedInCommentMessageResponseJson() throws RestApiException {
    String reviewMessageCommitMessage =
        getReviewMessage("__files/openai/openAIResponseRequest.json", 0);

    openAIPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("openAIResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAIUriResourceLocator.threadMessageRetrieveUri(
                        OPENAI_THREAD_ID, OPENAI_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("openai/openAIResponseThreadMessageJson.json")));

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }
}
