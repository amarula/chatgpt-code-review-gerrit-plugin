package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings.MODES;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistant.*;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatefulTest extends ChatGptReviewTestBase {
    private static final String CHAT_GPT_FILE_ID = "file-TEST_FILE_ID";
    private static final String CHAT_GPT_ASSISTANT_ID = "asst_TEST_ASSISTANT_ID";

    private ChatGptPromptStateful chatGptPromptStateful;

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

        // Mock the behavior of the ChatGPT create file request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.chatCreateFilesUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_FILE_ID + "}")));

        // Mock the behavior of the ChatGPT create assistant request
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo(URI.create(config.getGptDomain()
                        + UriResourceLocatorStateful.chatCreateAssistantsUri()).getPath()))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"id\": " + CHAT_GPT_ASSISTANT_ID + "}")));
    }

    protected void initComparisonContent() {
        super.initComparisonContent();
    }

    private void setupAssistantCreatedTest(Map<String, String> mockedPluginDataHandler) {
        // Mock the behavior of the Git Repository Manager
        String repoJson = readTestFile("__files/gitProjectFiles.json");
        when(gitRepoFiles.getGitRepoFiles(any())).thenReturn(repoJson);

        // Mock the behavior of the `setValue` method of the Plugin Data Handler
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            mockedPluginDataHandler.put(key, value);
            return null;  // since setValue is void
        }).when(pluginDataHandler).setValue(anyString(), anyString());
    }

    @Test
    public void assistantCreated() throws InterruptedException, ExecutionException {
        Map<String, String> mockedPluginDataHandler = new HashMap<>();
        setupAssistantCreatedTest(mockedPluginDataHandler);
        CompletableFuture<Void> future = handleEventBasedOnType(false);
        future.get();

        String projectFileIdKey = PROJECT_NAME + "." + KEY_FILE_ID;
        Assert.assertEquals(mockedPluginDataHandler.get(projectFileIdKey), CHAT_GPT_FILE_ID);
        String projectAssistantKey = PROJECT_NAME + "." + KEY_ASSISTANT_ID;
        Assert.assertEquals(mockedPluginDataHandler.get(projectAssistantKey), CHAT_GPT_ASSISTANT_ID);
    }

}
