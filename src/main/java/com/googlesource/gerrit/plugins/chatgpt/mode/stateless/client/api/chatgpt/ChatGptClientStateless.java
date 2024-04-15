package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.chatgpt;

import com.google.common.net.HttpHeaders;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http.HttpClientWithRetry;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.*;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getNoEscapedGson;

@Slf4j
@Singleton
public class ChatGptClientStateless implements IChatGptClient {
    private static final int REVIEW_ATTEMPT_LIMIT = 3;
    @Getter
    private String requestBody;
    private final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();
    private boolean isCommentEvent = false;

    @Override
    public String ask(Configuration config, String changeId, String patchSet) throws Exception {
        for (int attemptInd = 0; attemptInd < REVIEW_ATTEMPT_LIMIT; attemptInd++) {
            HttpRequest request = createRequest(config, changeId, patchSet);
            log.debug("ChatGPT request: {}", request.toString());

            HttpResponse<String> response = httpClientWithRetry.execute(request);

            String body = response.body();
            log.debug("body: {}", body);
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

    @Override
    public String ask(Configuration config, GerritChange change, String patchSet) throws Exception {
        isCommentEvent = change.getIsCommentEvent();

        return this.ask(config, change.getFullChangeId(), patchSet);
    }

    private String extractContent(Configuration config, String body) throws Exception {
        if (config.getGptStreamOutput() && !isCommentEvent) {
            StringBuilder finalContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    extractContentFromLine(line).ifPresent(finalContent::append);
                }
            }
            return finalContent.toString();
        }
        else {
            ChatGptResponseUnstreamed chatGptResponseUnstreamed =
                    getGson().fromJson(body, ChatGptResponseUnstreamed.class);
            return getResponseContent(chatGptResponseUnstreamed.getChoices().get(0).getMessage().getToolCalls());
        }
    }

    private boolean validateResponse(String contentExtracted, String changeId, int attemptInd) {
        ChatGptResponseContent chatGptResponseContent =
                getGson().fromJson(contentExtracted, ChatGptResponseContent.class);
        String returnedChangeId = chatGptResponseContent.getChangeId();
        // A response is considered valid if either no changeId is returned or the changeId returned matches the one
        // provided in the request
        boolean isValidated = returnedChangeId == null || changeId.equals(returnedChangeId);
        if (!isValidated) {
            log.error("ChangedId mismatch error (attempt #{}).\nExpected value: {}\nReturned value: {}", attemptInd,
                    changeId, returnedChangeId);
        }
        return isValidated;
    }

    private String getResponseContent(List<ChatGptToolCall> toolCalls) {
        return toolCalls.get(0).getFunction().getArguments();
    }

    private HttpRequest createRequest(Configuration config, String changeId, String patchSet) {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateless.chatCompletionsUri());
        log.debug("ChatGPT request URI: {}", uri);
        requestBody = createRequestBody(config, changeId, patchSet);
        log.debug("ChatGPT request body: {}", requestBody);

        return HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getGptToken())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String createRequestBody(Configuration config, String changeId, String patchSet) {
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, isCommentEvent);
        ChatGptRequestMessage systemMessage = ChatGptRequestMessage.builder()
                .role("system")
                .content(chatGptPromptStateless.getGptSystemPrompt())
                .build();
        ChatGptRequestMessage userMessage = ChatGptRequestMessage.builder()
                .role("user")
                .content(chatGptPromptStateless.getGptUserPrompt(patchSet, changeId))
                .build();

        ChatGptParameters chatGptParameters = new ChatGptParameters(config, isCommentEvent);
        ChatGptRequest tools = ChatGptTools.retrieveTools();
        ChatGptRequest chatGptRequest = ChatGptRequest.builder()
                .model(config.getGptModel())
                .messages(List.of(systemMessage, userMessage))
                .temperature(chatGptParameters.getGptTemperature())
                .stream(chatGptParameters.getStreamOutput())
                // Seed value is Utilized to prevent ChatGPT from mixing up separate API calls that occur in close
                // temporal proximity.
                .seed(chatGptParameters.getRandomSeed())
                .tools(tools.getTools())
                .toolChoice(tools.getToolChoice())
                .build();

        return getNoEscapedGson().toJson(chatGptRequest);
    }

    private Optional<String> extractContentFromLine(String line) {
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatGptResponseStreamed chatGptResponseStreamed =
                getGson().fromJson(line.substring("data: ".length()), ChatGptResponseStreamed.class);
        ChatGptResponseMessage delta = chatGptResponseStreamed.getChoices().get(0).getDelta();
        if (delta == null || delta.getToolCalls() == null) {
            return Optional.empty();
        }
        String content = getResponseContent(delta.getToolCalls());
        return Optional.ofNullable(content);
    }

}
