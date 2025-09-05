package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.api.changes.FileApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gson.JsonArray;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.http.entity.ContentType;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatGptReviewStatelessTestBase extends ChatGptReviewTestBase {

  protected ChatGptPromptStateless chatGptPromptStateless;
  protected JsonArray prompts;
  protected ReviewInput gerritPatchSetReview;

  protected void initConfig() {
    super.initConfig();
    when(globalConfig.getBoolean(Mockito.eq("gptStreamOutput"), Mockito.anyBoolean()))
        .thenReturn(GPT_STREAM_OUTPUT);
    chatGptPromptStateless = new ChatGptPromptStateless(config);
  }

  protected void initComparisonContent() {
    super.initComparisonContent();
    gerritPatchSetReview =
        readTestFileToClass("__files/stateless/gerritPatchSetReview.json", ReviewInput.class);
  }

  protected void mockChatCompletion(String fileName) {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo(UriResourceLocatorStateless.chatCompletionsUri()))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HTTP_OK)
                    .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                    .withBodyFile(fileName)));
  }

  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    // Mock the behavior of the gerritPatchSetFiles request
    Map<String, FileInfo> files =
        readTestFileToType(
            "__files/stateless/gerritPatchSetFiles.json",
            new TypeLiteral<Map<String, FileInfo>>() {}.getType());
    when(revisionApiMock.files(0)).thenReturn(files);

    // Mock the behavior of the gerritPatchSet diff requests
    FileApi commitMsgFileMock = mock(FileApi.class);
    when(revisionApiMock.file("/COMMIT_MSG")).thenReturn(commitMsgFileMock);
    DiffInfo commitMsgFileDiff =
        readTestFileToClass("__files/stateless/gerritPatchSetDiffCommitMsg.json", DiffInfo.class);
    when(commitMsgFileMock.diff(0)).thenReturn(commitMsgFileDiff);
    FileApi testFileMock = mock(FileApi.class);
    when(revisionApiMock.file("test_file.py")).thenReturn(testFileMock);
    DiffInfo testFileDiff =
        readTestFileToClass("__files/stateless/gerritPatchSetDiffTestFile.json", DiffInfo.class);
    when(testFileMock.diff(0)).thenReturn(testFileDiff);

    // Mock the behavior of the askGpt request
    mockChatCompletion("chatGptResponseStreamed.txt");
  }

  protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
    ArgumentCaptor<ReviewInput> reviewInputCaptor = super.testRequestSent();
    prompts = gptRequestBody.get("messages").getAsJsonArray();
    return reviewInputCaptor;
  }

  protected String getUserPrompt() {
    return prompts.get(1).getAsJsonObject().get("content").getAsString();
  }
}
