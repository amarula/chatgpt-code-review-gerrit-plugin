package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonObject;
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


import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptRun.COMPLETED_STATUS;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatefulTest extends ChatGptReviewTestBase {
    private static final String CHAT_GPT_FILE_ID = "file-TEST_FILE_ID";
    private static final String CHAT_GPT_ASSISTANT_ID = "asst_TEST_ASSISTANT_ID";
    private static final String CHAT_GPT_THREAD_ID = "thread_TEST_THREAD_ID";
    private static final String CHAT_GPT_MESSAGE_ID = "msg_TEST_MESSAGE_ID";
    private static final String CHAT_GPT_RUN_ID = "run_TEST_RUN_ID";

    private String formattedPatchContent;
    private String reviewMessage;
    private ChatGptPromptStateful chatGptPromptStateful;
    private JsonObject threadMessage;

    public ChatGptReviewStatefulTest() {
        MockitoAnnotations.openMocks(this);
    }

    protected void initGlobalAndProjectConfig() {
        super.initGlobalAndProjectConfig();

        // Mock the Global Config values that differ from the ones provided by Default
        when(globalConfig.getString(Mockito.eq("gptMode"), Mockito.anyString()))
                .thenReturn(MODES.stateful.name());
    }

    protected void initConfig() {
        super.initConfig();

        // Load the prompts
        chatGptPromptStateful = new ChatGptPromptStateful(config, getGerritChange());
    }

    protected void setupMockRequests() throws RestApiException {
        super.setupMockRequests();

        // Mock the behavior of the Git Repository Manager
        String repoJson = readTestFile("__files/stateful/gitProjectFiles.json");
        when(gitRepoFiles.getGitRepoFiles(any())).thenReturn(repoJson);

        // Mock the behavior of the ChatGPT create-file request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.filesCreateUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_FILE_ID + "}")));

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
    }

    protected void initComparisonContent() {
        super.initComparisonContent();

        reviewMessage = getReviewMessage();
    }

    protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
        ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
        threadMessage = gptRequestBody.getAsJsonObject();
        return reviewInputCaptor;
    }

    private String getReviewMessage() {
        ChatGptListResponse reviewResponse = getGson().fromJson(readTestFile(
                "__files/chatGptRunStepsResponse.json"
        ), ChatGptListResponse.class);
        String reviewJsonResponse = reviewResponse.getData().get(0).getStepDetails().getToolCalls().get(0).getFunction()
                .getArguments();
        return getGson().fromJson(reviewJsonResponse, ChatGptResponseContent.class).getReplies().get(0).getReply();
    }

    @Test
    public void patchSetCreatedOrUpdated() throws Exception {
        String reviewUserPrompt = chatGptPromptStateful.getDefaultGptThreadReviewMessage(formattedPatchContent);

        handleEventBasedOnType(false);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        String userPrompt = threadMessage.get("content").getAsString();
        Assert.assertEquals(reviewUserPrompt, userPrompt);
        Assert.assertEquals(
                reviewMessage,
                captor.getAllValues().get(0).comments.get(GERRIT_PATCH_SET_FILENAME).get(0).message
        );
    }

}
