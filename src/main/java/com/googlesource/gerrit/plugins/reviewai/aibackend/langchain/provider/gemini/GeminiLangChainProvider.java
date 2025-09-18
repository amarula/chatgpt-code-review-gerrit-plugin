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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.gemini;

import static com.googlesource.gerrit.plugins.reviewai.config.Configuration.GEMINI_DOMAIN;
import static com.googlesource.gerrit.plugins.reviewai.config.Configuration.OPENAI_DOMAIN;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.model.LangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.langchain.provider.ILangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeminiLangChainProvider implements ILangChainProvider {

  private static final String TOKEN_ESTIMATOR_CLASS =
      "dev.langchain4j.model.googleai.GoogleAiTokenCountEstimator";
  private static volatile Boolean estimatorAvailable;

  @Override
  public LangChainProvider buildChatModel(Configuration config, double temperature) {
    String endpoint = config.getAiDomain();
    if (endpoint == null || endpoint.isBlank() || OPENAI_DOMAIN.equals(endpoint)) {
      endpoint = GEMINI_DOMAIN;
    }

    ChatModel model =
        GoogleAiGeminiChatModel.builder()
            .apiKey(config.getAiToken())
            .modelName(config.getAiModel())
            .temperature(temperature)
            .timeout(Duration.ofSeconds(config.getAiConnectionTimeout()))
            .build();

    return new LangChainProvider(model, endpoint);
  }

  @Override
  public Optional<TokenCountEstimator> createTokenEstimator(Configuration config) {
    if (Boolean.FALSE.equals(estimatorAvailable)) {
      return Optional.empty();
    }
    try {
      String token = config.getAiToken();
      Class<?> estimatorClass = Class.forName(TOKEN_ESTIMATOR_CLASS);
      Object builder = estimatorClass.getMethod("builder").invoke(null);
      builder.getClass().getMethod("apiKey", String.class).invoke(builder, token);
      builder.getClass().getMethod("modelName", String.class).invoke(builder, config.getAiModel());
      Object estimator = builder.getClass().getMethod("build").invoke(builder);
      estimatorAvailable = true;
      return Optional.of((TokenCountEstimator) estimator);
    } catch (ClassNotFoundException e) {
      estimatorAvailable = false;
      log.info(
          "Google Gemini token estimator class not present on classpath. Falling back to approximate estimator.");
      return Optional.empty();
    } catch (RuntimeException e) {
      estimatorAvailable = false;
      log.warn(
          "aiToken is not configured. Falling back to approximate token estimator: {}",
          e.getMessage());
      return Optional.empty();
    } catch (Throwable t) {
      estimatorAvailable = false;
      log.warn(
          "Google Gemini token estimator unavailable for model {}. Using approximate estimator.",
          config.getAiModel(),
          t);
      return Optional.empty();
    }
  }
}
