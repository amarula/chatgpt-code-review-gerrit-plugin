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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.client.api;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.messages.LangChainMessageTextExtractor;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.LangChainProviderFactory;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.langchain.provider.ILangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.TokenCountEstimator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class LangChainTokenEstimatorProvider {

  private static final long TOKEN_ESTIMATOR_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
  private static final TokenCountEstimator APPROXIMATE_ESTIMATOR =
      new ApproximateTokenCountEstimator();

  private final Configuration config;

  private volatile TokenCountEstimator cachedEstimator;

  LangChainTokenEstimatorProvider(Configuration config) {
    this.config = config;
  }

  TokenCountEstimator get() {
    TokenCountEstimator current = cachedEstimator;
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (cachedEstimator != null) {
        return cachedEstimator;
      }
      LangChainProviders provider = config.getLcProvider();
      try {
        log.info(
            "Initializing {} token estimator for model {}", provider, config.getAiModel());
        TokenCountEstimator estimator =
            CompletableFuture.supplyAsync(() -> createEstimator(provider))
                .get(TOKEN_ESTIMATOR_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        cachedEstimator = estimator;
        log.info("Initialized {} token estimator for model {}", provider, config.getAiModel());
      } catch (Exception e) {
        log.warn(
            "Failed to initialize {} token estimator for model {}. Using approximate estimator.",
            provider,
            config.getAiModel(),
            e);
        cachedEstimator = APPROXIMATE_ESTIMATOR;
      }
      return cachedEstimator;
    }
  }

  private TokenCountEstimator createEstimator(LangChainProviders provider) {
    ILangChainProvider adapter = LangChainProviderFactory.get(provider);
    return adapter.createTokenEstimator(config).orElse(APPROXIMATE_ESTIMATOR);
  }

  private static final class ApproximateTokenCountEstimator implements TokenCountEstimator {
    @Override
    public int estimateTokenCountInText(String text) {
      if (text == null || text.isEmpty()) {
        return 0;
      }
      return Math.max(1, text.length() / 4);
    }

    @Override
    public int estimateTokenCountInMessage(ChatMessage message) {
      if (message == null) {
        return 0;
      }
      return estimateTokenCountInText(LangChainMessageTextExtractor.extractText(message)) + 4;
    }

    @Override
    public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
      int total = 0;
      if (messages == null) {
        return 0;
      }
      for (ChatMessage message : messages) {
        total += estimateTokenCountInMessage(message);
      }
      return total;
    }
  }
}
