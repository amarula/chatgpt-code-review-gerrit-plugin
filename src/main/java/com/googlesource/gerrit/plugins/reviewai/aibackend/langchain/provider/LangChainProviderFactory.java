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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.gemini.GeminiLangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.openai.OpenAiLangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.langchain.provider.ILangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;

public final class LangChainProviderFactory {

  private static final ILangChainProvider OPENAI_PROVIDER = new OpenAiLangChainProvider();
  private static final ILangChainProvider GEMINI_PROVIDER = new GeminiLangChainProvider();

  private LangChainProviderFactory() {}

  public static ILangChainProvider get(LangChainProviders provider) {
    return switch (provider) {
      case OPENAI -> OPENAI_PROVIDER;
      case GEMINI -> GEMINI_PROVIDER;
    };
  }
}
