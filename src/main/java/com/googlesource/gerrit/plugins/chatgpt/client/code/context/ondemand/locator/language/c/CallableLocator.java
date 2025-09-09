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

package com.googlesource.gerrit.plugins.chatgpt.client.code.context.ondemand.locator.language.c;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.client.code.context.ondemand.locator.CallableLocatorBase;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CallableLocator extends CallableLocatorBase implements IEntityLocator {
  private static final String C_SOURCE_EXTENSION = ".c";
  private static final String C_HEADER_EXTENSION = ".h";

  private final Set<String> includeFiles = new HashSet<>();

  public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config, change, gitRepoFiles);
    log.debug("Initializing CallableLocator for C language");
    languageModuleExtensions = new String[] {C_SOURCE_EXTENSION, C_HEADER_EXTENSION};
    importPattern = Pattern.compile("^\\s*#include\\s+[\"<](.*?)[\">]", Pattern.MULTILINE);
  }

  @Override
  protected String getFunctionRegex(String functionName) {
    return "^\\s*(?:[\\w\\*\\s]+)?\\s+"
        + // Return type
        Pattern.quote(functionName)
        + "\\s*\\([^;]*\\)"; // Parameters
  }

  @Override
  protected void parseImportStatements(String content) {
    log.debug("Parsing include statements");
    Matcher includeMatcher = importPattern.matcher(content);
    while (includeMatcher.find()) {
      String includeFile = includeMatcher.group(1);
      log.debug("Found include file: {}", includeFile);
      includeFiles.add(includeFile);
    }
    log.debug("Found include files: {}", includeFiles);
  }

  @Override
  protected String convertModuleToPath(String module, String languageModuleExtension) {
    return module;
  }

  @Override
  protected String findInImportModules(String functionName) {
    // First, try to find the function in included header files
    for (String headerFile : includeFiles) {
      if (!headerFile.endsWith(C_HEADER_EXTENSION)) {
        headerFile += C_HEADER_EXTENSION;
      }
      log.debug("Searching for function {} in header file {}", functionName, headerFile);
      String result = getFunctionFromModule(functionName, headerFile);
      if (result != null) return result;
    }
    // If not found, proceed to check other source files (.c files)
    return findInModules(functionName);
  }
}
