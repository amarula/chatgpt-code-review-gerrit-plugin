package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.json.OutputFormat;
import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatelessTest extends ChatGptReviewTestBase {
    private ReviewInput expectedResponseStreamed;
    private String expectedSystemPromptReview;
    private String promptTagReview;
    private String promptTagComments;
    private String diffContent;
    private ReviewInput gerritPatchSetReview;

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

        String fullChangeId = getGerritChange().getFullChangeId();

        // Mock the behavior of the gerritPatchSetFiles request
        WireMock.stubFor(WireMock.get(UriResourceLocatorStateless.gerritPatchSetFilesUri(fullChangeId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("gerritPatchSetFiles.json")));

        // Mock the behavior of the gerritPatchSet diff requests
        WireMock.stubFor(WireMock.get(UriResourceLocatorStateless.gerritPatchSetFilesUri(fullChangeId) +
                            UriResourceLocatorStateless.gerritDiffPostfixUri("/COMMIT_MSG"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("gerritPatchSetDiffCommitMsg.json")));
        WireMock.stubFor(WireMock.get(UriResourceLocatorStateless.gerritPatchSetFilesUri(fullChangeId) +
                            UriResourceLocatorStateless.gerritDiffPostfixUri("test_file.py"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("gerritPatchSetDiffTestFile.json")));

        // Mock the behavior of the askGpt request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateless.chatCompletionsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("chatGptResponseStreamed.txt")));

        // Mock the GerritApi's revision API
        when(changeApiMock.current()).thenReturn(revisionApiMock);
    }

    protected void initComparisonContent() {
        super.initComparisonContent();

        diffContent = readTestFile("reducePatchSet/patchSetDiffOutput.json");
        gerritPatchSetReview = readTestFileToClass("__files/gerritPatchSetReview.json", ReviewInput.class);
        expectedResponseStreamed = readTestFileToClass("__files/chatGptExpectedResponseStreamed.json", ReviewInput.class);
        promptTagReview = readTestFile("__files/chatGptPromptTagReview.json");
        promptTagComments = readTestFile("__files/chatGptPromptTagRequests.json");
        expectedSystemPromptReview = ChatGptPromptStateless.getDefaultGptReviewSystemPrompt();
    }

    private String getReviewUserPrompt() {
        return joinWithNewLine(Arrays.asList(
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT,
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_REVIEW + " " +
                        chatGptPromptStateless.getPatchSetReviewUserPrompt(),
                ChatGptPromptStateless.DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES,
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

        handleEventBasedOnType(false);

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

        handleEventBasedOnType(false);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(reviewUserPrompt, userPrompt);

        Gson gson = OutputFormat.JSON_COMPACT.newGson();
        Assert.assertEquals(gson.toJson(gerritPatchSetReview), gson.toJson(captor.getAllValues().get(0)));
    }

    @Test
    public void patchSetDisableUserGroup() {
        when(globalConfig.getString(Mockito.eq("disabledGroups"), Mockito.anyString()))
                .thenReturn(GERRIT_USER_GROUP);

        Assert.assertEquals(EventHandlerTask.Result.NOT_SUPPORTED, handleEventBasedOnType(false));
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
                        .withBodyFile("chatGptResponseRequests.json")));

        handleEventBasedOnType(true);
        int commentPropertiesSize = gerritClient.getClientData(getGerritChange()).getCommentProperties().size();

        String commentUserPrompt = joinWithNewLine(Arrays.asList(
                ChatGptPromptStateless.DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                diffContent,
                ChatGptPromptStateless.DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                promptTagComments,
                ChatGptPromptStateless.getCommentRequestUserPrompt(commentPropertiesSize)
        ));
        testRequestSent();
        String userPrompt = prompts.get(1).getAsJsonObject().get("content").getAsString();
        Assert.assertEquals(commentUserPrompt, userPrompt);
    }

}
