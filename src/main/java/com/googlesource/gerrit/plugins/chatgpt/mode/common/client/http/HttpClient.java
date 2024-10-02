package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class HttpClient {
    private final OkHttpClient client;
    private final String bearer;
    private final String domain;

    public HttpClient(Configuration config) {
        this.bearer = config.getGptToken();
        this.domain = config.getGptDomain();
        int connectionTimeout = config.getGptConnectionTimeout();
        HttpRetryInterceptor httpRetryInterceptor = new HttpRetryInterceptor(
                config.getGptConnectionMaxRetryAttempts(),
                config.getGptConnectionRetryInterval()
        );
        this.client = new OkHttpClient.Builder()
                .addInterceptor(httpRetryInterceptor)
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                .readTimeout(connectionTimeout, TimeUnit.SECONDS)
                .writeTimeout(connectionTimeout, TimeUnit.SECONDS)
                .build();
    }

    public String execute(Request request) throws OpenAiConnectionFailException {
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
            throw new OpenAiConnectionFailException(e);
        }
        return null;
    }

    public Request createRequest(String uri, RequestBody body, Map<String, String> additionalHeaders) {
        // If body is null, a GET request is initiated. Otherwise, a POST request is sent with the specified body.
        uri = domain + uri;
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

    public Request createRequestFromJson(String uri, Object requestObject, Map<String, String> additionalHeaders) {
        if (requestObject != null) {
            String bodyJson = getGson().toJson(requestObject);
            log.debug("Creating JSON request body: {}", bodyJson);
            RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

            return createRequest(uri, body, additionalHeaders);
        }
        else {
            log.debug("Creating request without a body for URI: {}", uri);
            return createRequest(uri, null, additionalHeaders);
        }
    }

    public Request createRequestFromJson(String uri, Object requestObject) {
        return createRequestFromJson(uri, requestObject, null);
    }
}
