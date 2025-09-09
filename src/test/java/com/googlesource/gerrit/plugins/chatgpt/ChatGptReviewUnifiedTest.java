package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonArray;
import com.googlesource.gerrit.plugins.chatgpt.client.api.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.prompt.ChatGptPromptReviewReiterated;
import com.googlesource.gerrit.plugins.chatgpt.utils.ThreadUtils;
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

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptPoller.FAILED_STATUS;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewUnifiedTest extends ChatGptReviewTestBase {
  private static final String CHAT_GPT_ASSISTANT_ID = "asst_TEST_ASSISTANT_ID";
  private static final String CHAT_GPT_RUN_ID = "run_TEST_RUN_ID";

  @Rule public TestName testName = new TestName();

  @Override
  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    setupMockRequestCreateAssistant(CHAT_GPT_ASSISTANT_ID);
    setupMockRequestCreateRun(CHAT_GPT_ASSISTANT_ID, CHAT_GPT_RUN_ID);
    setupMockRequestRetrieveRunSteps("chatGptRunStepsResponse.json");

    if ("vectorStoreCreateFailure".equals(testName.getMethodName())) {
      when(globalConfig.getInt(Mockito.eq("gptPollingTimeout"), Mockito.anyInt())).thenReturn(0);
    }
  }

  protected void setupMockRequestRetrieveRunSteps(String bodyFile) {
    setupMockRequestRetrieveRunSteps(bodyFile, CHAT_GPT_RUN_ID);
  }

  protected String getUserPrompt() {
    JsonArray prompts = readContentToType(gptRequestBody.get("content").getAsString(), JsonArray.class);
    return prompts.get(0).getAsJsonObject().get("request").getAsString();
  }

  private void setupVectorStoreFailure() {
    // Mock the behavior of the ChatGPT create-vector-store-file-batch request with failure
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    UriResourceLocator.vectorStoreFileBatchCreateUri(
                        CHAT_GPT_VECTOR_STORE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(
                        "{\"id\": "
                            + CHAT_GPT_VECTOR_STORE_FILE_BATCH_ID
                            + ", \"status\": "
                            + FAILED_STATUS
                            + "}")));
    when(globalConfig.getInt(Mockito.eq("gptPollingInterval"), Mockito.anyInt())).thenReturn(0);
  }

  @Test
  public void patchSetCreatedOrUpdated() throws Exception {
    String reviewMessageCode = getReviewMessage("__files/chatGptRunStepsResponse.json", 0);
    String reviewMessageCommitMessage =
        getReviewMessage("__files/chatGptRunStepsResponse.json", 1);

    String reviewPrompt =
        chatGptPrompt.getDefaultGptThreadReviewMessage(formattedPatchContent);

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
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.filesCreateUri()))
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
    String reviewMessageCode = getReviewMessage("__files/chatGptRunStepsResponse.json", 0);
    setupVectorStoreFailure();

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
  }

  @Test
  public void vectorStoreCreateFailure() {
    setupVectorStoreFailure();
    // Mock the behavior of the ChatGPT retrieve-vector-store-file-batch request with failure
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    UriResourceLocator.vectorStoreFileBatchRetrieveUri(
                        CHAT_GPT_VECTOR_STORE_ID, CHAT_GPT_VECTOR_STORE_FILE_BATCH_ID)))
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
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.threadsUri()))
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
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.runsUri(CHAT_GPT_THREAD_ID)))
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
    // To effectively test how an initial empty response from ChatGPT is managed, the following
    // approach is adopted:
    // 1. the ChatGPT run-steps request is initially mocked to return an empty data field, and
    // 2. the sleep function is mocked to replace the empty response with a valid one, instead of
    // pausing execution
    setupMockRequestRetrieveRunSteps("chatGptRunStepsEmptyResponse.json");

    try (MockedStatic<ThreadUtils> mocked = Mockito.mockStatic(ThreadUtils.class)) {
      mocked
          .when(() -> ThreadUtils.threadSleep(Mockito.anyLong()))
          .thenAnswer(
              invocation -> {
                setupMockRequestRetrieveRunSteps("chatGptRunStepsResponse.json");
                return null;
              });

      String reviewMessageCode =
          getReviewMessage("__files/chatGptRunStepsResponse.json", 0);
      String reviewMessageCommitMessage =
          getReviewMessage("__files/chatGptRunStepsResponse.json", 1);

      String reviewPrompt =
          chatGptPrompt.getDefaultGptThreadReviewMessage(formattedPatchContent);

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
                    UriResourceLocator.runStepsUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)))
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
        new ChatGptPromptReviewReiterated(
                config, changeSetData, getGerritChange(), getCodeContextPolicy())
            .getDefaultGptThreadReviewMessage("");

    setupMockRequestRetrieveRunSteps("chatGptResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.threadMessageRetrieveUri(
                        CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("chatGptResponseThreadMessageText.json")));

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    testRequestSent();
    Assert.assertEquals(reviewReiteratePrompt, requestContent);
  }

  @Test
  public void patchSetCreatedReiterateRequestForMalformedJson() throws Exception {
    String reviewReiteratePrompt =
        new ChatGptPromptReviewReiterated(
                config, changeSetData, getGerritChange(), getCodeContextPolicy())
            .getDefaultGptThreadReviewMessage("");

    setupMockRequestRetrieveRunSteps("chatGptRunStepsResponseMalformedJson.json");

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    testRequestSent();
    Assert.assertEquals(reviewReiteratePrompt, requestContent);
  }

  @Test
  public void gptMentionedInComment() throws RestApiException {
    String reviewMessageCommitMessage =
        getReviewMessage("__files/chatGptResponseRequest.json", 0);

    chatGptPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("chatGptResponseRequest.json");

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }

  @Test
  public void gptMentionedInCommentMessageResponseText() throws RestApiException {
    String reviewMessageCommitMessage =
        getReviewMessage("__files/chatGptResponseRequest.json", 0);

    chatGptPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("chatGptResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.threadMessageRetrieveUri(
                        CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("chatGptResponseThreadMessageText.json")));

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }

  @Test
  public void gptMentionedInCommentMessageResponseText400() {
    chatGptPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("chatGptResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.threadMessageRetrieveUri(
                        CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)))
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
        getReviewMessage("__files/chatGptResponseRequest.json", 0);

    chatGptPrompt.setCommentEvent(true);
    setupMockRequestRetrieveRunSteps("chatGptResponseRequestMessage.json");
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.threadMessageRetrieveUri(
                        CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile("chatGptResponseThreadMessageJson.json")));

    handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    Assert.assertEquals(promptTagComments, requestContent);
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }
}
