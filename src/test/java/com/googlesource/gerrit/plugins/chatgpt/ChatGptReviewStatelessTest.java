package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask;
import com.google.gerrit.extensions.api.changes.FileApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.json.OutputFormat;
import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatelessTest extends ChatGptReviewTestBase {
    private ReviewInput expectedResponseStreamed;
    private String expectedSystemPromptReview;
    private String promptTagReview;
    private String diffContent;
    private ReviewInput gerritPatchSetReview;
    private JsonArray prompts;

    private ChatGptPromptStateless chatGptPromptStateless;

    protected void initConfig() {
        super.initGlobalAndProjectConfig();

        when(globalConfig.getBoolean(Mockito.eq("gptStreamOutput"), Mockito.anyBoolean()))
                .thenReturn(GPT_STREAM_OUTPUT);
        when(globalConfig.getBoolean(Mockito.eq("gptReviewCommitMessages"), Mockito.anyBoolean()))
                .thenReturn(true);

        super.initConfig();

        // Load the prompts
        chatGptPromptStateless = new ChatGptPromptStateless(config);
    }

    protected void setupMockRequests() throws RestApiException {
        super.setupMockRequests();

        // Mock the behavior of the gerritPatchSetFiles request
        Map<String, FileInfo> files = readTestFileToType(
                "__files/stateless/gerritPatchSetFiles.json",
                new TypeLiteral<Map<String, FileInfo>>() {}.getType()
        );
        when(revisionApiMock.files(0)).thenReturn(files);

        // Mock the behavior of the gerritPatchSet diff requests
        FileApi commitMsgFileMock = mock(FileApi.class);
        when(revisionApiMock.file("/COMMIT_MSG")).thenReturn(commitMsgFileMock);
        DiffInfo commitMsgFileDiff = readTestFileToClass("__files/stateless/gerritPatchSetDiffCommitMsg.json", DiffInfo.class);
        when(commitMsgFileMock.diff(0)).thenReturn(commitMsgFileDiff);
        FileApi testFileMock = mock(FileApi.class);
        when(revisionApiMock.file("test_file.py")).thenReturn(testFileMock);
        DiffInfo testFileDiff = readTestFileToClass("__files/stateless/gerritPatchSetDiffTestFile.json", DiffInfo.class);
        when(testFileMock.diff(0)).thenReturn(testFileDiff);

        // Mock the behavior of the askGpt request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateless.chatCompletionsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseStreamed.txt")));
    }

    protected void initComparisonContent() {
        super.initComparisonContent();

        diffContent = readTestFile("reducePatchSet/patchSetDiffOutput.json");
        gerritPatchSetReview = readTestFileToClass("__files/stateless/gerritPatchSetReview.json", ReviewInput.class);
        expectedResponseStreamed = readTestFileToClass("__files/stateless/chatGptExpectedResponseStreamed.json", ReviewInput.class);
        promptTagReview = readTestFile("__files/stateless/chatGptPromptTagReview.json");
        promptTagComments = readTestFile("__files/stateless/chatGptPromptTagRequests.json");
        expectedSystemPromptReview = ChatGptPromptStateless.getDefaultGptReviewSystemPrompt();
    }

    protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
        ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
        prompts = gptRequestBody.get("messages").getAsJsonArray();
        return reviewInputCaptor;
    }

    private String getReviewUserPrompt() {
        return joinWithNewLine(Arrays.asList(
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT,
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_REVIEW + " " +
                        ChatGptPromptStateless.DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT + " " +
                        chatGptPromptStateless.getPatchSetReviewPrompt(),
                ChatGptPromptStateless.getReviewPromptCommitMessages(),
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_DIFF,
                diffContent,
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY,
                promptTagReview
        ));
    }

    @Test
    public void patchSetCreatedOrUpdatedStreamed() throws Exception {
        String reviewUserPrompt = getReviewUserPrompt();
        chatGptPromptStateless.setCommentEvent(false);

        handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        String systemPrompt = prompts.get(0).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(expectedSystemPromptReview, systemPrompt);
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(reviewUserPrompt, userPrompt);

        Gson gson = OutputFormat.JSON_COMPACT.newGson();
        Assert.assertEquals(gson.toJson(expectedResponseStreamed), gson.toJson(captor.getAllValues().get(0)));
    }

    @Test
    public void patchSetCreatedOrUpdatedUnstreamed() throws Exception {
        when(globalConfig.getBoolean(Mockito.eq("gptStreamOutput"), Mockito.anyBoolean()))
                .thenReturn(false);
        when(globalConfig.getBoolean(Mockito.eq("enabledVoting"), Mockito.anyBoolean()))
                .thenReturn(true);

        String reviewUserPrompt = getReviewUserPrompt();
        chatGptPromptStateless.setCommentEvent(false);
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateless.chatCompletionsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseReview.json")));

        handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(reviewUserPrompt, userPrompt);

        Gson gson = OutputFormat.JSON_COMPACT.newGson();
        Assert.assertEquals(gson.toJson(gerritPatchSetReview), gson.toJson(captor.getAllValues().get(0)));
    }

    @Test
    public void patchSetCreatedOrUpdatedResponse400() throws Exception {
        when(globalConfig.getBoolean(Mockito.eq("gptStreamOutput"), Mockito.anyBoolean()))
                .thenReturn(false);

        String reviewUserPrompt = getReviewUserPrompt();
        chatGptPromptStateless.setCommentEvent(false);
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(UriResourceLocatorStateless.chatCompletionsUri()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_BAD_REQUEST)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())));

        handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

        testRequestSent();
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(reviewUserPrompt, userPrompt);

        Assert.assertEquals(localizer.getText("message.openai.connection.error"), changeSetData.getReviewSystemMessage());
    }

    @Test
    public void patchSetDisableUserGroup() {
        when(globalConfig.getString(Mockito.eq("disabledGroups"), Mockito.anyString()))
                .thenReturn(GERRIT_USER_GROUP);

        Assert.assertEquals(EventHandlerTask.Result.NOT_SUPPORTED, handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED));
    }

    @Test
    public void gptMentionedInComment() throws RestApiException {
        when(config.getGerritUserName()).thenReturn(GERRIT_GPT_USERNAME);
        chatGptPromptStateless.setCommentEvent(true);
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateless.chatCompletionsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseRequestStateless.json")));

        handleEventBasedOnType(SupportedEvents.COMMENT_ADDED);
        int commentPropertiesSize = gerritClient.getClientData(getGerritChange()).getCommentProperties().size();

        String commentUserPrompt = joinWithNewLine(Arrays.asList(
                ChatGptPromptStateless.DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                diffContent,
                ChatGptPromptStateless.DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                readTestFile("__files/stateless/chatGptExpectedRequestMessage.json"),
                ChatGptPromptStateless.getCommentRequestPrompt(commentPropertiesSize)
        ));
        testRequestSent();
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(commentUserPrompt, userPrompt);
    }
}
