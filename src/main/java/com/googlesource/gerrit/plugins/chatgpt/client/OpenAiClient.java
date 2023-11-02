package com.googlesource.gerrit.plugins.chatgpt.client;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
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

@Slf4j
@Singleton
public class OpenAiClient {
    private final Gson gson = new Gson();
    private final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();

    public String ask(Configuration config, String patchSet) throws Exception {
        HttpRequest request = createRequest(config, patchSet);
        log.debug("request: {}", request.toString());

        HttpResponse<String> response = httpClientWithRetry.execute(request);

        String body = response.body();
        log.debug("body: {}", body);
        if (body == null) {
            throw new IOException("responseBody is null");
        }
        String content = extractContent(config, body);
        log.debug("content: {}", content);

        return content;
    }

    public String extractContent(Configuration config, String body) throws Exception {
        if (config.getGptStreamOutput()) {
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
            ChatCompletionResponseMessage chatCompletionResponseMessage =
                    gson.fromJson(body, ChatCompletionResponseMessage.class);
            return chatCompletionResponseMessage.getChoices().get(0).getMessage().getContent();
        }
    }

    private HttpRequest createRequest(Configuration config, String patchSet) {
        String requestBody = createRequestBody(config, patchSet);
        URI uri = URI.create(URI.create(config.getGptDomain()) + UriResourceLocator.chatCompletionsUri());
        log.debug("GPT request URI: {}", uri);
        log.debug("GPT requestBody: {}", requestBody);

        return HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getGptToken())
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String createRequestBody(Configuration config, String patchSet) {
        ChatCompletionRequest.Message systemMessage = ChatCompletionRequest.Message.builder()
                .role("system")
                .content(config.getGptPrompt())
                .build();
        ChatCompletionRequest.Message userMessage = ChatCompletionRequest.Message.builder()
                .role("user")
                .content(patchSet)
                .build();

        List<ChatCompletionRequest.Message> messages = List.of(systemMessage, userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(config.getGptModel())
                .messages(messages)
                .temperature(config.getGptTemperature())
                .stream(config.getGptStreamOutput())
                .build();

        return gson.toJson(chatCompletionRequest);
    }

    private Optional<String> extractContentFromLine(String line) {
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatCompletionResponse chatCompletionResponse =
                gson.fromJson(line.substring("data: ".length()), ChatCompletionResponse.class);
        String content = chatCompletionResponse.getChoices().get(0).getDelta().getContent();
        return Optional.ofNullable(content);
    }

}
