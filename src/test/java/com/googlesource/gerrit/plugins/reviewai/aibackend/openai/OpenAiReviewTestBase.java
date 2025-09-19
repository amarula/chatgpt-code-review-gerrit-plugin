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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.FileApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonArray;
import com.googlesource.gerrit.plugins.reviewai.ReviewTestBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiResponseContent;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAiUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiListResponse;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.prompt.IAiPrompt;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.OpenAiPoller.COMPLETED_STATUS;
import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.jsonToClass;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class OpenAiReviewTestBase extends ReviewTestBase {
  protected static final String OPENAI_FILE_ID = "file-TEST_FILE_ID";
  protected static final String OPENAI_VECTOR_STORE_ID = "vs-TEST_VECTOR_STORE_ID";
  protected static final String OPENAI_VECTOR_STORE_FILE_BATCH_ID =
      "vsfb-TEST_VECTOR_STORE_FILE_BATCH_ID";
  protected static final String OPENAI_THREAD_ID = "thread_TEST_THREAD_ID";
  protected static final String OPENAI_MESSAGE_ID = "msg_TEST_MESSAGE_ID";
  protected static final String OPENAI_RUN_ID = "run_TEST_RUN_ID";
  protected static final String RESOURCE_OPENAI_PATH = "__files/openai/";

  protected String formattedPatchContent;
  protected IAiPrompt openAiPrompt;
  protected String requestContent;
  protected PluginDataHandler projectHandler;

  public OpenAiReviewTestBase() {
    MockitoAnnotations.openMocks(this);
  }

  protected void initGlobalAndProjectConfig() {
    super.initGlobalAndProjectConfig();

    setupPluginData();
    PluginDataHandlerProvider provider =
        new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
    projectHandler = provider.getProjectScope();
    // Mock the pluginDataHandlerProvider to return the mocked project pluginDataHandler
    when(pluginDataHandlerProvider.getProjectScope()).thenReturn(projectHandler);
    // Mock the pluginDataHandlerProvider to return the mocked assistant pluginDataHandler
    when(pluginDataHandlerProvider.getAssistantsWorkspace()).thenReturn(projectHandler);
  }

  protected void initTest() {
    super.initTest();

    // Load the prompts
    openAiPrompt =
        AiPromptFactory.getAiPrompt(
            config, changeSetData, getGerritChange(), getCodeContextPolicy());
  }

  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    // Mock the behavior of the Git Repository Manager
    String repoJson = readTestFile(RESOURCE_OPENAI_PATH + "gitProjectFiles.json");
    when(gitRepoFiles.getGitRepoFilesAsJson(any(), any())).thenReturn(List.of(repoJson));

    // Mock the behavior of the OpenAI create-file request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAiUriResourceLocator.filesCreateUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + OPENAI_FILE_ID + "}")));

    // Mock the behavior of the OpenAI create-vector-store request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAiUriResourceLocator.vectorStoreCreateUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + OPENAI_VECTOR_STORE_ID + "}")));

    // Mock the behavior of the OpenAI create-vector-store-file-batch request
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    OpenAiUriResourceLocator.vectorStoreFileBatchCreateUri(OPENAI_VECTOR_STORE_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(
                        "{\"id\": "
                            + OPENAI_VECTOR_STORE_FILE_BATCH_ID
                            + ", \"status\": "
                            + COMPLETED_STATUS
                            + "}")));

    // Mock the behavior of the OpenAI retrieve-vector-store-file-batch request
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    OpenAiUriResourceLocator.vectorStoreFileBatchRetrieveUri(
                        OPENAI_VECTOR_STORE_ID, OPENAI_VECTOR_STORE_FILE_BATCH_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"status\": " + COMPLETED_STATUS + "}")));

    // Mock the behavior of the OpenAI create-thread request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(OpenAiUriResourceLocator.threadsUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + OPENAI_THREAD_ID + "}")));

    // Mock the behavior of the OpenAI add-message-to-thread request
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(OpenAiUriResourceLocator.threadMessagesUri(OPENAI_THREAD_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + OPENAI_MESSAGE_ID + "}")));

    // Mock the behavior of the formatted patch request
    formattedPatchContent = readTestFile(RESOURCE_OPENAI_PATH + "gerritFormattedPatch.txt");
    ByteArrayInputStream inputStream = new ByteArrayInputStream(formattedPatchContent.getBytes());
    BinaryResult binaryResult =
        BinaryResult.create(inputStream)
            .setContentType("text/plain")
            .setContentLength(formattedPatchContent.length());
    when(revisionApiMock.patch()).thenReturn(binaryResult);

    FileApi testFileMock = mock(FileApi.class);
    when(revisionApiMock.file("test_file_1.py")).thenReturn(testFileMock);
    DiffInfo testFileDiff =
        readTestFileToClass(
            RESOURCE_OPENAI_PATH + "gerritPatchSetDiffTestFile.json", DiffInfo.class);
    when(testFileMock.diff(0)).thenReturn(testFileDiff);
  }

  protected void initComparisonContent() {
    super.initComparisonContent();

    promptTagComments = readTestFile(RESOURCE_OPENAI_PATH + "openAiPromptTagRequests.json");
  }

  protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
    ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
    requestContent = aiRequestBody.getAsJsonObject().get("content").getAsString();
    return reviewInputCaptor;
  }

  protected String getReviewMessage(String responseFile, int tollCallId) {
    OpenAiListResponse responseContent =
        jsonToClass(readTestFile(responseFile), OpenAiListResponse.class);
    String reviewJsonResponse =
        responseContent
            .getData()
            .get(0)
            .getStepDetails()
            .getToolCalls()
            .get(tollCallId)
            .getFunction()
            .getArguments();
    return jsonToClass(reviewJsonResponse, AiResponseContent.class)
        .getReplies()
        .get(0)
        .getReply();
  }

  protected List<ReviewInput.CommentInput> getCapturedComments(
      ArgumentCaptor<ReviewInput> captor, String filename) {
    return captor.getAllValues().get(0).comments.get(filename);
  }

  protected String getCapturedMessage(ArgumentCaptor<ReviewInput> captor, String filename) {
    return getCapturedComments(captor, filename).get(0).message;
  }

  protected void setupMockRequestCreateAssistant(
      String assistantId, String fromState, String toState) {
    // Mock the behavior of the OpenAI create-assistant request
    WireMock.stubFor(
        getScenarioMapping(
                OpenAiUriResourceLocator.assistantCreateUri(),
                "Assistant Scenario",
                fromState,
                toState)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + assistantId + "}")));
  }

  protected void setupMockRequestCreateAssistant(String assistantId, String fromState) {
    setupMockRequestCreateAssistant(assistantId, fromState, null);
  }

  protected void setupMockRequestCreateAssistant(String assistantId) {
    setupMockRequestCreateAssistant(assistantId, null, null);
  }

  protected void setupMockRequestCreateRun(
      String assistantId, String runId, String fromState, String toState) {
    // Mock the behavior of the OpenAI create-run request
    WireMock.stubFor(
        getScenarioMapping(
                OpenAiUriResourceLocator.runsUri(OPENAI_THREAD_ID),
                "Create-Run Scenario",
                fromState,
                toState)
            .withRequestBody(equalToJson(getJsonAssistantId(assistantId), true, true))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + runId + ", \"status\": " + COMPLETED_STATUS + "}")));
  }

  protected void setupMockRequestCreateRun(String assistantId, String runId, String fromState) {
    setupMockRequestCreateRun(assistantId, runId, fromState, null);
  }

  protected void setupMockRequestCreateRun(String assistantId, String runId) {
    setupMockRequestCreateRun(assistantId, runId, null, null);
  }

  protected void setupMockRequestRetrieveRunStepsFromBody(String body, String runId) {
    // Mock the behavior of the OpenAI retrieve-run-steps request
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(OpenAiUriResourceLocator.runStepsUri(OPENAI_THREAD_ID, runId)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(body)));
  }

  protected void setupMockRequestRetrieveRunSteps(String bodyFile, String runId) {
    setupMockRequestRetrieveRunStepsFromBody(readTestFile(RESOURCE_OPENAI_PATH + bodyFile), runId);
  }

  protected void setupMockRequestRetrieveRunSteps(String bodyFile) {
    setupMockRequestRetrieveRunSteps(bodyFile, OPENAI_RUN_ID);
  }

  private MappingBuilder getScenarioMapping(
      String resourceURI, String scenario, String fromState, String toState) {
    MappingBuilder mappingBuilder = WireMock.post(WireMock.urlEqualTo(resourceURI));
    if (fromState != null) {
      mappingBuilder =
          mappingBuilder
              .inScenario(scenario)
              .whenScenarioStateIs(fromState)
              .willSetStateTo(toState);
    }
    return mappingBuilder;
  }

  private String getJsonAssistantId(String assistantId) {
    return getGson().toJson(Collections.singletonMap("assistant_id", assistantId));
  }

  protected String getUserPrompt() {
    JsonArray prompts =
        readContentToType(aiRequestBody.get("content").getAsString(), JsonArray.class);
    return prompts.get(0).getAsJsonObject().get("request").getAsString();
  }
}
