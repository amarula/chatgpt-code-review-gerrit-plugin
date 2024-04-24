package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings.MODES;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.URI;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.*;

@Slf4j
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

    protected void setupMockRequests() {
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

}
