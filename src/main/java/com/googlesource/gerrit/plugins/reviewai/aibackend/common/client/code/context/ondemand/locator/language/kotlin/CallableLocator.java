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

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.code.context.ondemand.locator.language.kotlin;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.code.context.ondemand.locator.CallableLocatorJVM;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CallableLocator extends CallableLocatorJVM implements IEntityLocator {
  private static final String KOTLIN_MODULE_EXTENSION = ".kt";
  private static final String ALTERNATIVE_BASE_PATH = "app/src/main/kotlin/";
  private static final String NON_MODIFIABLE_BASE_PATH = "app/src/";

  public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config, change, gitRepoFiles);
    log.debug("Initializing CallableLocator for Kotlin projects");
    languageModuleExtensions = new String[] {KOTLIN_MODULE_EXTENSION};
    importPattern =
        Pattern.compile(String.format("^import\\s+(%s)", DOT_NOTATION_REGEX), Pattern.MULTILINE);
  }

  @Override
  protected String getFunctionRegex(String functionName) {
    return "^\\s*(?:@\\w+(?:\\(.*?\\))?\\s*)*"
        + // Optional annotations
        "(?:\\w+\\s+)*"
        + // Optional modifiers
        "fun\\s+"
        + // 'fun' keyword
        Pattern.quote(functionName)
        + // Function name
        "\\s*\\(.*?\\)"
        + // Parameters
        "(?:\\s*:\\s*\\S+)?"; // Optional return type
  }

  @Override
  protected void parseImportStatements(String content) {
    parseDirectImportStatements(content);
    Set<String> alternativePathModules =
        importModules.stream()
            .filter(s -> !s.startsWith(NON_MODIFIABLE_BASE_PATH))
            .map(s -> ALTERNATIVE_BASE_PATH + s)
            .collect(Collectors.toSet());
    importModules.addAll(alternativePathModules);
  }
}
