/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.http;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Slf4j
public class HttpClient {
  private final OkHttpClient client;
  private final String bearer;
  private final String domain;

  public HttpClient(Configuration config) {
    this.bearer = config.getAiToken();
    this.domain = config.getAiDomain();
    int connectionTimeout = config.getAiConnectionTimeout();
    HttpRetryInterceptor httpRetryInterceptor =
        new HttpRetryInterceptor(
            config.getAiConnectionMaxRetryAttempts(), config.getAiConnectionRetryInterval());
    this.client =
        new OkHttpClient.Builder()
            .addInterceptor(httpRetryInterceptor)
            .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
            .readTimeout(connectionTimeout, TimeUnit.SECONDS)
            .writeTimeout(connectionTimeout, TimeUnit.SECONDS)
            .build();
  }

  public String execute(Request request) throws AiConnectionFailException {
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
      } else {
        log.error("Request {} returned an empty response body", request);
      }
    } catch (IOException e) {
      log.error("HTTP request execution failed for request URL: {}", request.url(), e);
      throw new AiConnectionFailException(e);
    }
    return null;
  }

  public Request createRequest(
      String uri, RequestBody body, Map<String, String> additionalHeaders) {
    // If body is null, a GET request is initiated. Otherwise, a POST request is sent with the
    // specified body.
    uri = domain + uri;
    Request.Builder builder =
        new Request.Builder().url(uri).header("Authorization", "Bearer " + bearer);

    if (body != null) {
      builder.post(body);
      log.debug("Creating POST request for URI: {} with body", uri);
    } else {
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

  public Request createRequestFromJson(
      String uri, Object requestObject, Map<String, String> additionalHeaders) {
    if (requestObject != null) {
      String bodyJson = getGson().toJson(requestObject);
      log.debug("Creating JSON request body: {}", bodyJson);
      RequestBody body = RequestBody.create(bodyJson, MediaType.get("application/json"));

      return createRequest(uri, body, additionalHeaders);
    } else {
      log.debug("Creating request without a body for URI: {}", uri);
      return createRequest(uri, null, additionalHeaders);
    }
  }

  public Request createRequestFromJson(String uri, Object requestObject) {
    return createRequestFromJson(uri, requestObject, null);
  }
}
