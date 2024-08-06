package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.CHAT_GPT_CONNECTION_TIMEOUT;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class HttpClient {
    private final OkHttpClient client;

    public HttpClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CHAT_GPT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(CHAT_GPT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(CHAT_GPT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    public String execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("HTTP request failed with status code: {}", response.code());
                throw new IOException("Unexpected code " + response);
            }
            log.debug("HTTP response successfully received for request URL: {}", request.url());
            if (response.body() != null) {
                String responseBody = response.body().string();
                log.debug("HTTP Response body for request URL {}: {}", request.url(), responseBody);
                return responseBody;
            }
            else {
                log.error("Request {} returned an empty response body", request);
            }
        } catch (IOException e) {
            log.error("HTTP request execution failed for request URL: {}", request.url(), e);
        }
        return null;
    }

    public Request createRequest(String uri, String bearer, RequestBody body, Map<String, String> additionalHeaders) {
        // If body is null, a GET request is initiated. Otherwise, a POST request is sent with the specified body.
        Request.Builder builder = new Request.Builder()
                .url(uri)
                .header("Authorization", "Bearer " + bearer);

        if (body != null) {
            builder.post(body);
            log.debug("Creating POST request for URI: {} with body", uri);
        }
        else {
            builder.get();
            log.debug("Creating GET request for URI: {}", uri);
        }
        if (additionalHeaders != null) {
            for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
                builder.header(header.getKey(), header.getValue());
                log.debug("Added header {} : {}", header.getKey(), header.getValue());
            }
        }
        return builder.build();
    }

    public Request createRequestFromJson(String uri, String bearer, Object requestObject,
                                         Map<String, String> additionalHeaders) {
        if (requestObject != null) {
            String bodyJson = getGson().toJson(requestObject);
            log.debug("Creating JSON request body: {}", bodyJson);
            RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

            return createRequest(uri, bearer, body, additionalHeaders);
        }
        else {
            log.debug("Creating request without a body for URI: {}", uri);
            return createRequest(uri, bearer, null, additionalHeaders);
        }
    }
}
