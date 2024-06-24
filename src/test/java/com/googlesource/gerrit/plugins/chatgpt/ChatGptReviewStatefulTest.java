package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.FileApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptListResponse;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings.MODES;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptRun.COMPLETED_STATUS;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStore.KEY_VECTOR_STORE_ID;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatefulTest extends ChatGptReviewTestBase {
    private static final String CHAT_GPT_FILE_ID = "file-TEST_FILE_ID";
    private static final String CHAT_GPT_VECTOR_ID = "file-TEST_VECTOR_ID";
    private static final String CHAT_GPT_ASSISTANT_ID = "asst_TEST_ASSISTANT_ID";
    private static final String CHAT_GPT_THREAD_ID = "thread_TEST_THREAD_ID";
    private static final String CHAT_GPT_MESSAGE_ID = "msg_TEST_MESSAGE_ID";
    private static final String CHAT_GPT_RUN_ID = "run_TEST_RUN_ID";

    private String formattedPatchContent;
    private ChatGptPromptStateful chatGptPromptStateful;
    private String requestContent;
    private PluginDataHandler projectHandler;

    public ChatGptReviewStatefulTest() {
        MockitoAnnotations.openMocks(this);
    }

    protected void initGlobalAndProjectConfig() {
        super.initGlobalAndProjectConfig();

        // Mock the Global Config values that differ from the ones provided by Default
        when(globalConfig.getString(Mockito.eq("gptMode"), Mockito.anyString()))
                .thenReturn(MODES.stateful.name());

        setupPluginData();
        PluginDataHandlerProvider provider = new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
        projectHandler = provider.getProjectScope();
        // Mock the pluginDataHandlerProvider to return the mocked project pluginDataHandler
        when(pluginDataHandlerProvider.getProjectScope()).thenReturn(projectHandler);
    }

    protected void initTest() {
        super.initTest();

        // Load the prompts
        chatGptPromptStateful = new ChatGptPromptStateful(config, changeSetData, getGerritChange());
    }

    protected void setupMockRequests() throws RestApiException {
        super.setupMockRequests();

        // Mock the behavior of the Git Repository Manager
        String repoJson = readTestFile("__files/stateful/gitProjectFiles.json");
        when(gitRepoFiles.getGitRepoFiles(any(), any())).thenReturn(repoJson);

        // Mock the behavior of the ChatGPT create-file request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.filesCreateUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_FILE_ID + "}")));

        // Mock the behavior of the ChatGPT create-vector-store request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.vectorStoreCreateUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_VECTOR_ID + "}")));

        // Mock the behavior of the ChatGPT create-assistant request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.assistantCreateUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_ASSISTANT_ID + "}")));

        // Mock the behavior of the ChatGPT create-thread request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.threadsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_THREAD_ID + "}")));

        // Mock the behavior of the ChatGPT add-message-to-thread request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.threadMessagesUri(CHAT_GPT_THREAD_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_MESSAGE_ID + "}")));

        // Mock the behavior of the ChatGPT create-run request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runsUri(CHAT_GPT_THREAD_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_RUN_ID + "}")));

        // Mock the behavior of the ChatGPT retrieve-run request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runRetrieveUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"status\": " + COMPLETED_STATUS + "}")));

        // Mock the behavior of the ChatGPT retrieve-run-steps request
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runStepsUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptRunStepsResponse.json")));

        // Mock the behavior of the formatted patch request
        formattedPatchContent = readTestFile("__files/stateful/gerritFormattedPatch.txt");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(formattedPatchContent.getBytes());
        BinaryResult binaryResult = BinaryResult.create(inputStream)
                .setContentType("text/plain")
                .setContentLength(formattedPatchContent.length());
        when(revisionApiMock.patch()).thenReturn(binaryResult);

        FileApi testFileMock = mock(FileApi.class);
        when(revisionApiMock.file("test_file_1.py")).thenReturn(testFileMock);
        DiffInfo testFileDiff = readTestFileToClass("__files/stateful/gerritPatchSetDiffTestFile.json", DiffInfo.class);
        when(testFileMock.diff(0)).thenReturn(testFileDiff);
    }

    protected void initComparisonContent() {
        super.initComparisonContent();

        promptTagComments = readTestFile("__files/stateful/chatGptPromptTagRequests.json");
    }

    protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
        ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
        requestContent = gptRequestBody.getAsJsonObject().get("content").getAsString();
        return reviewInputCaptor;
    }

    private String getReviewMessage(String responseFile, int tollCallId) {
        ChatGptListResponse responseContent = getGson().fromJson(readTestFile(responseFile), ChatGptListResponse.class);
        String reviewJsonResponse = responseContent.getData().get(0).getStepDetails().getToolCalls().get(tollCallId)
                .getFunction().getArguments();
        return getGson().fromJson(reviewJsonResponse, ChatGptResponseContent.class).getReplies().get(0).getReply();
    }

    private String getCapturedMessage(ArgumentCaptor<ReviewInput> captor, String filename) {
        return captor.getAllValues().get(0).comments.get(filename).get(0).message;
    }

    @Test
    public void patchSetCreatedOrUpdated() throws Exception {
        String reviewMessageCode = getReviewMessage( "__files/chatGptRunStepsResponse.json", 0);
        String reviewMessageCommitMessage = getReviewMessage( "__files/chatGptRunStepsResponse.json", 1);

        String reviewUserPrompt = chatGptPromptStateful.getDefaultGptThreadReviewMessage(formattedPatchContent);

        handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        Assert.assertEquals(reviewUserPrompt, requestContent);
        Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
        Assert.assertEquals(reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }

    @Test
    public void gptMentionedInComment() throws RestApiException {
        String reviewMessageCommitMessage = getReviewMessage("__files/chatGptResponseRequestStateful.json", 0);

        chatGptPromptStateful.setCommentEvent(true);
        // Mock the behavior of the ChatGPT retrieve-run-steps request
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runStepsUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseRequestStateful.json")));

        handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        Assert.assertEquals(promptTagComments, requestContent);
        Assert.assertEquals(reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }

    @Test
    public void gptMentionedInCommentMessageResponseText() throws RestApiException {
        String reviewMessageCommitMessage = getReviewMessage("__files/chatGptResponseRequestStateful.json", 0);

        chatGptPromptStateful.setCommentEvent(true);
        // Mock the behavior of the ChatGPT retrieve-run-steps request
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runStepsUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseRequestMessageStateful.json")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.threadMessageRetrieveUri(CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseThreadMessageText.json")));

        handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        Assert.assertEquals(promptTagComments, requestContent);
        Assert.assertEquals(reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }

    @Test
    public void gptMentionedInCommentMessageResponseJson() throws RestApiException {
        String reviewMessageCommitMessage = getReviewMessage("__files/chatGptResponseRequestStateful.json", 0);

        chatGptPromptStateful.setCommentEvent(true);
        // Mock the behavior of the ChatGPT retrieve-run-steps request
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.runStepsUri(CHAT_GPT_THREAD_ID, CHAT_GPT_RUN_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseRequestMessageStateful.json")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.threadMessageRetrieveUri(CHAT_GPT_THREAD_ID, CHAT_GPT_MESSAGE_ID)).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseThreadMessageJson.json")));

        handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        Assert.assertEquals(promptTagComments, requestContent);
        Assert.assertEquals(reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }

    @Test
    public void gerritMergedCommits() {
        projectHandler.removeValue(KEY_VECTOR_STORE_ID);
        handleEventBasedOnType(SupportedEvents.CHANGE_MERGED);

        Assert.assertEquals(CHAT_GPT_VECTOR_ID, projectHandler.getValue(KEY_VECTOR_STORE_ID));
    }
}
