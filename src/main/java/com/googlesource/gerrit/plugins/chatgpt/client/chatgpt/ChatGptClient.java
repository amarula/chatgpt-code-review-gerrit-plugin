package com.googlesource.gerrit.plugins.chatgpt.client.chatgpt;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.client.HttpClientWithRetry;
import com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.model.chatGpt.*;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
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
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Singleton
public class ChatGptClient {
    private static final int REVIEW_ATTEMPT_LIMIT = 3;
    @Getter
    private String requestBody;
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    private final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();
    private ChatGptTools chatGptTools;
    private boolean isCommentEvent = false;

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

    public String ask(Configuration config, GerritChange change, String patchSet) throws Exception {
        isCommentEvent = change.getIsCommentEvent();
        chatGptTools = new ChatGptTools(config, isCommentEvent);

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
                    gson.fromJson(body, ChatGptResponseUnstreamed.class);
            return getResponseContent(chatGptResponseUnstreamed.getChoices().get(0).getMessage().getToolCalls());
        }
    }

    private boolean validateResponse(String contentExtracted, String changeId, int attemptInd) {
        ChatGptResponseContent chatGptResponseContent = gson.fromJson(contentExtracted, ChatGptResponseContent.class);
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
        URI uri = URI.create(URI.create(config.getGptDomain()) + UriResourceLocator.chatCompletionsUri());
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
        ChatGptRequest.Message systemMessage = ChatGptRequest.Message.builder()
                .role("system")
                .content(config.getGptSystemPrompt())
                .build();
        ChatGptRequest.Message userMessage = ChatGptRequest.Message.builder()
                .role("user")
                .content(config.getGptUserPrompt(patchSet, changeId))
                .build();

        List<ChatGptRequest.Message> messages = List.of(systemMessage, userMessage);
        ChatGptRequest tools = chatGptTools.retrieveTools(changeId);
        ChatGptRequest chatGptRequest = ChatGptRequest.builder()
                .model(config.getGptModel())
                .messages(messages)
                .temperature(config.getGptTemperature())
                .stream(config.getGptStreamOutput() && !isCommentEvent)
                // Seed value is Utilized to prevent ChatGPT from mixing up separate API calls that occur in close
                // temporal proximity.
                .seed(ThreadLocalRandom.current().nextInt())
                .tools(tools.getTools())
                .toolChoice(tools.getToolChoice())
                .build();

        return gson.toJson(chatGptRequest);
    }

    private Optional<String> extractContentFromLine(String line) {
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatGptResponseStreamed chatGptResponseStreamed =
                gson.fromJson(line.substring("data: ".length()), ChatGptResponseStreamed.class);
        ChatGptResponseMessage delta = chatGptResponseStreamed.getChoices().get(0).getDelta();
        if (delta == null || delta.getToolCalls() == null) {
            return Optional.empty();
        }
        String content = getResponseContent(delta.getToolCalls());
        return Optional.ofNullable(content);
    }

}
