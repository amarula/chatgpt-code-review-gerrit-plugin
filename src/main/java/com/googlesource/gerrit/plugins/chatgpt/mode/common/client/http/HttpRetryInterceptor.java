package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.http;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class HttpRetryInterceptor implements Interceptor {
    private final int maxRetries;
    private final long retryInterval;

    public HttpRetryInterceptor(int maxRetries, long retryInterval) {
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval * 1000;
    }

    @Override
    public @NonNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        int retryIndex = 1;

        while (true) {
            try {
                response = chain.proceed(request);
                if (response.isSuccessful()) {
                    return response;
                } else {
                    log.error("Retry because HTTP status code is not 200. The status code is: {}", response.code());
                    response.close();
                }
            } catch (IOException| IllegalStateException e) {
                log.error("Retry failed with exception: {}", e.getMessage());
            }

            if (retryIndex >= maxRetries) {
                break;
            }
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Retry interrupted", ie);
            }
            retryIndex++;
        }

        if (response == null) {
            throw new IOException("Connection timed out");
        }
        else {
            throw new IOException("Unexpected response code " + response.code());
        }
    }
}
