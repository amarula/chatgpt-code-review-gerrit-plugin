package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.chatgpt;

import com.google.common.net.HttpHeaders;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClientWithRetry;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.*;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.model.api.chatgpt.ChatGptCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getNoEscapedGson;

@Slf4j
@Singleton
public class ChatGptClientStateless extends ChatGptClient implements IChatGptClient {
    private static final int REVIEW_ATTEMPT_LIMIT = 3;

    private final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();

    public String ask(Configuration config, ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws Exception {
        isCommentEvent = change.getIsCommentEvent();
        String changeId = change.getFullChangeId();
        log.info("Processing STATELESS ChatGPT Request with changeId: {}, Patch Set: {}", changeId, patchSet);
        for (int attemptInd = 0; attemptInd < REVIEW_ATTEMPT_LIMIT; attemptInd++) {
            HttpRequest request = createRequest(config, changeSetData, patchSet);
            log.debug("ChatGPT request: {}", request.toString());

            HttpResponse<String> response = httpClientWithRetry.execute(request);

            String body = response.body();
            log.debug("ChatGPT response body: {}", body);
            if (body == null) {
                throw new IOException("ChatGPT response body is null");
            }

            String contentExtracted = extractContent(config, body);
            if (validateResponse(contentExtracted, changeId, attemptInd)) {
                return contentExtracted;
            }
        }
        throw new RuntimeException("Failed to receive valid ChatGPT response");
    }

    protected HttpRequest createRequest(Configuration config, ChangeSetData changeSetData, String patchSet) {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateless.chatCompletionsUri());
        log.debug("ChatGPT request URI: {}", uri);
        requestBody = createRequestBody(config, changeSetData, patchSet);
        log.debug("ChatGPT request body: {}", requestBody);

        return HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getGptToken())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String createRequestBody(Configuration config, ChangeSetData changeSetData, String patchSet) {
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, isCommentEvent);
        ChatGptRequestMessage systemMessage = ChatGptRequestMessage.builder()
                .role("system")
                .content(chatGptPromptStateless.getGptSystemPrompt())
                .build();
        ChatGptRequestMessage userMessage = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateless.getGptUserPrompt(changeSetData, patchSet))
                .build();

        ChatGptParameters chatGptParameters = new ChatGptParameters(config, isCommentEvent);
        ChatGptTool[] tools = new ChatGptTool[] {
                ChatGptTools.retrieveFormatRepliesTool()
        };
        ChatGptCompletionRequest chatGptCompletionRequest = ChatGptCompletionRequest.builder()
                .model(config.getGptModel())
                .messages(List.of(systemMessage, userMessage))
                .temperature(chatGptParameters.getGptTemperature())
                .stream(chatGptParameters.getStreamOutput())
                // Seed value is Utilized to prevent ChatGPT from mixing up separate API calls that occur in close
                // temporal proximity.
                .seed(chatGptParameters.getRandomSeed())
                .tools(tools)
                .toolChoice(ChatGptTools.retrieveFormatRepliesToolChoice())
                .build();

        return getNoEscapedGson().toJson(chatGptCompletionRequest);
    }

}
