package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.HttpClientWithRetry;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static java.net.HttpURLConnection.HTTP_OK;


@Slf4j
public abstract class GerritClientBase extends ClientBase {
    protected final HttpClientWithRetry httpClientWithRetry = new HttpClientWithRetry();
    @Getter
    protected HashMap<String, FileDiffProcessed> fileDiffsProcessed = new HashMap<>();

    public GerritClientBase(Configuration config) {
        super(config);
    }

    protected String generateBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    protected String forwardGetRequest(URI uri) throws Exception {
        log.debug("Uri: {}", uri);
        HttpRequest request = HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, generateBasicAuth(config.getGerritUserName(),
                        config.getGerritPassword()))
                .uri(uri)
                .build();

        HttpResponse<String> response = httpClientWithRetry.execute(request);

        if (response.statusCode() != HTTP_OK) {
            log.error("Failed to get response from Gerrit. Response: {}", response);
            throw new IOException("Failed to get response from Gerrit");
        }
        log.debug("Successfully obtained response from Gerrit.");

        return response.body();
    }

    protected JsonArray forwardGetRequestReturnJsonArray(URI uri) throws Exception {
        String responseBody = forwardGetRequest(uri);
        return getGson().fromJson(responseBody, JsonArray.class);
    }

    protected JsonObject forwardGetRequestReturnJsonObject(URI uri) throws Exception {
        String responseBody = forwardGetRequest(uri);
        return getGson().fromJson(responseBody, JsonObject.class);
    }

}
