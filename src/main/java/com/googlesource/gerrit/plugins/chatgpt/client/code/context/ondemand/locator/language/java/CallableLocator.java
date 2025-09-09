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

package com.googlesource.gerrit.plugins.chatgpt.client.code.context.ondemand.locator.language.java;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.client.code.context.ondemand.locator.CallableLocatorJVM;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class CallableLocator extends CallableLocatorJVM implements IEntityLocator {
  private static final String JAVA_MODULE_EXTENSION = ".java";

  public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config, change, gitRepoFiles);
    log.debug("Initializing CallableLocator for Java projects");
    languageModuleExtensions = new String[] {JAVA_MODULE_EXTENSION};
    importPattern =
        Pattern.compile(
            String.format("^import\\s+(?:static\\s+)?(%s)", DOT_NOTATION_REGEX), Pattern.MULTILINE);
  }

  @Override
  protected String getFunctionRegex(String functionName) {
    return "^\\s*(?:@\\w+(?:\\(.*?\\))?\\s*)*"
        + // Optional annotations
        "(?:(?:public|protected|private|static|final|abstract|synchronized|native|strictfp)\\s+)*"
        + // Optional modifiers
        "(?:<[^>]+>\\s*)?"
        + // Optional type parameters
        "\\S+\\s+"
        + // Return type
        Pattern.quote(functionName)
        + // Method name
        "\\s*\\(.*?\\)"
        + // Parameters
        "(?:\\s*throws\\s+[^\\{;]+)?"; // Optional throws clause
  }
}
