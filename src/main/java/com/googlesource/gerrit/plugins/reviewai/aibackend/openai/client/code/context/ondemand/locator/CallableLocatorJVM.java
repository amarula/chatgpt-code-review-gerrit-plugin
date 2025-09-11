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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.ondemand.locator;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
public abstract class CallableLocatorJVM extends CallableLocatorBase implements IEntityLocator {
  public CallableLocatorJVM(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config, change, gitRepoFiles);
    log.debug("Initializing JVM CallableLocator");
  }

  @Override
  protected void parseImportStatements(String content) {
    parseDirectImportStatements(content);
  }

  @Override
  protected String findInImportModules(String functionName) {
    return findInModules(functionName);
  }

  protected void parseDirectImportStatements(String content) {
    log.debug("Parsing import statements");
    Matcher importMatcher = importPattern.matcher(content);
    while (importMatcher.find()) {
      String importModulesGroup = importMatcher.group(1);
      log.debug("Parsing import module: `{}`", importModulesGroup);
      importModules.add(importModulesGroup);
    }
    log.debug("Found import modules from import statements: {}", importModules);
  }

  @Override
  protected void beforeSearchingFunction() {
    log.debug("Retrieving modules from current package");
    Set<String> packageModules =
        codeFileFetcher.getFilesInDir(rootFileDir).stream()
            .map(FileUtils::removeExtension)
            .collect(Collectors.toSet());
    log.debug("Modules retrieved from current package: {}", packageModules);
    importModules.addAll(packageModules);
  }
}
