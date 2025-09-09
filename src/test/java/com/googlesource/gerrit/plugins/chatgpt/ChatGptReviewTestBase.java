package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.FileApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.chatgpt.client.prompt.ChatGptPromptFactory;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.prompt.IChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.client.api.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptListResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.googlesource.gerrit.plugins.chatgpt.client.api.chatgpt.ChatGptPoller.COMPLETED_STATUS;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.jsonToClass;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ChatGptReviewTestBase extends ReviewTestBase {
  protected static final String CHAT_GPT_FILE_ID = "file-TEST_FILE_ID";
  protected static final String CHAT_GPT_VECTOR_STORE_ID = "vs-TEST_VECTOR_STORE_ID";
  protected static final String CHAT_GPT_VECTOR_STORE_FILE_BATCH_ID =
      "vsfb-TEST_VECTOR_STORE_FILE_BATCH_ID";
  protected static final String CHAT_GPT_THREAD_ID = "thread_TEST_THREAD_ID";
  protected static final String CHAT_GPT_MESSAGE_ID = "msg_TEST_MESSAGE_ID";
  protected static final String RESOURCE_PATH = "__files/";

  protected String formattedPatchContent;
  protected IChatGptPrompt chatGptPrompt;
  protected String requestContent;
  protected PluginDataHandler projectHandler;

  public ChatGptReviewTestBase() {
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
    chatGptPrompt =
        ChatGptPromptFactory.getChatGptPrompt(config, changeSetData, getGerritChange(), getCodeContextPolicy());
  }

  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    // Mock the behavior of the Git Repository Manager
    String repoJson = readTestFile("__files/gitProjectFiles.json");
    when(gitRepoFiles.getGitRepoFilesAsJson(any(), any())).thenReturn(List.of(repoJson));

    // Mock the behavior of the ChatGPT create-file request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.filesCreateUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + CHAT_GPT_FILE_ID + "}")));

    // Mock the behavior of the ChatGPT create-vector-store request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.vectorStoreCreateUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + CHAT_GPT_VECTOR_STORE_ID + "}")));

    // Mock the behavior of the ChatGPT create-vector-store-file-batch request
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
                            + COMPLETED_STATUS
                            + "}")));

    // Mock the behavior of the ChatGPT retrieve-vector-store-file-batch request
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.vectorStoreFileBatchRetrieveUri(
                        CHAT_GPT_VECTOR_STORE_ID, CHAT_GPT_VECTOR_STORE_FILE_BATCH_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"status\": " + COMPLETED_STATUS + "}")));

    // Mock the behavior of the ChatGPT create-thread request
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(UriResourceLocator.threadsUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + CHAT_GPT_THREAD_ID + "}")));

    // Mock the behavior of the ChatGPT add-message-to-thread request
    WireMock.stubFor(
        WireMock.post(
                WireMock.urlEqualTo(
                    UriResourceLocator.threadMessagesUri(CHAT_GPT_THREAD_ID)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody("{\"id\": " + CHAT_GPT_MESSAGE_ID + "}")));

    // Mock the behavior of the formatted patch request
    formattedPatchContent = readTestFile("__files/gerritFormattedPatch.txt");
    ByteArrayInputStream inputStream = new ByteArrayInputStream(formattedPatchContent.getBytes());
    BinaryResult binaryResult =
        BinaryResult.create(inputStream)
            .setContentType("text/plain")
            .setContentLength(formattedPatchContent.length());
    when(revisionApiMock.patch()).thenReturn(binaryResult);

    FileApi testFileMock = mock(FileApi.class);
    when(revisionApiMock.file("test_file_1.py")).thenReturn(testFileMock);
    DiffInfo testFileDiff =
        readTestFileToClass("__files/gerritPatchSetDiffTestFile.json", DiffInfo.class);
    when(testFileMock.diff(0)).thenReturn(testFileDiff);
  }

  protected void initComparisonContent() {
    super.initComparisonContent();

    promptTagComments = readTestFile("__files/chatGptPromptTagRequests.json");
  }

  protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
    ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
    requestContent = gptRequestBody.getAsJsonObject().get("content").getAsString();
    return reviewInputCaptor;
  }

  protected String getReviewMessage(String responseFile, int tollCallId) {
    ChatGptListResponse responseContent =
        jsonToClass(readTestFile(responseFile), ChatGptListResponse.class);
    String reviewJsonResponse =
        responseContent
            .getData()
            .get(0)
            .getStepDetails()
            .getToolCalls()
            .get(tollCallId)
            .getFunction()
            .getArguments();
    return jsonToClass(reviewJsonResponse, ChatGptResponseContent.class)
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
    // Mock the behavior of the ChatGPT create-assistant request
    WireMock.stubFor(
        getScenarioMapping(
                UriResourceLocator.assistantCreateUri(),
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
    // Mock the behavior of the ChatGPT create-run request
    WireMock.stubFor(
        getScenarioMapping(
                UriResourceLocator.runsUri(CHAT_GPT_THREAD_ID),
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
    // Mock the behavior of the ChatGPT retrieve-run-steps request
    WireMock.stubFor(
        WireMock.get(
                WireMock.urlEqualTo(
                    UriResourceLocator.runStepsUri(CHAT_GPT_THREAD_ID, runId)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBody(body)));
  }

  protected void setupMockRequestRetrieveRunSteps(String bodyFile, String runId) {
    setupMockRequestRetrieveRunStepsFromBody(
        readTestFile(RESOURCE_PATH + bodyFile), runId);
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
}
