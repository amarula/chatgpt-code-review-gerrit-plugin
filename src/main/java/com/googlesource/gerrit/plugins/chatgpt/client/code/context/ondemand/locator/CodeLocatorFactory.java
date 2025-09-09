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

package com.googlesource.gerrit.plugins.chatgpt.client.code.context.ondemand.locator;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.CodeContextOnDemandLocatorException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptGetContextItem;
import com.googlesource.gerrit.plugins.chatgpt.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.getExtension;
import static com.googlesource.gerrit.plugins.chatgpt.utils.ModuleUtils.joinComponents;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.convertSnakeToPascalCase;

@Slf4j
public class CodeLocatorFactory {
  public static final String LANGUAGE_PACKAGE = "language";

  public enum EntityCategory {
    Callable,
    Data,
    Type
  }

  private static final Map<String, String> MAP_EXTENSION =
      Map.of(
          "py", "python",
          "java", "java",
          "kt", "kotlin",
          "c", "c",
          "h", "c");

  private static List<String> classNameComponents;

  public static IEntityLocator getEntityLocator(
      ChatGptGetContextItem chatGptGetContextItem,
      Configuration config,
      GerritChange change,
      GitRepoFiles gitRepoFiles)
      throws CodeContextOnDemandLocatorException {
    log.debug("Getting Entity Locator for context item {}", chatGptGetContextItem);
    IEntityLocator entityLocator;
    classNameComponents =
        new ArrayList<>(List.of(CodeLocatorFactory.class.getPackage().getName(), LANGUAGE_PACKAGE));
    addProgrammingLanguage(chatGptGetContextItem);
    addLocatorTypeClass(chatGptGetContextItem);
    String className = joinComponents(classNameComponents);
    log.debug("Entity locator class name found: {}", className);
    try {
      log.debug("Getting Instance of Entity Locator");
      @SuppressWarnings("unchecked")
      Class<IEntityLocator> clazz = (Class<IEntityLocator>) Class.forName(className);
      log.debug("Class found: {}", clazz);
      entityLocator =
          clazz
              .getDeclaredConstructor(Configuration.class, GerritChange.class, GitRepoFiles.class)
              .newInstance(config, change, gitRepoFiles);
    } catch (ClassNotFoundException e) {
      log.warn("Entity locator class not found for Get-Context Item: {}", chatGptGetContextItem);
      throw new CodeContextOnDemandLocatorException(e);
    } catch (InvocationTargetException
        | InstantiationException
        | IllegalAccessException
        | NoSuchMethodException e) {
      log.error("Error instantiating class: {}", className, e);
      throw new CodeContextOnDemandLocatorException(e);
    }
    return entityLocator;
  }

  private static void addProgrammingLanguage(ChatGptGetContextItem chatGptGetContextItem)
      throws CodeContextOnDemandLocatorException {
    String language = getCodeLocatorLanguage(chatGptGetContextItem);
    if (language == null) {
      log.warn("No language supported for file {}", chatGptGetContextItem.getFilename());
      throw new CodeContextOnDemandLocatorException();
    }
    classNameComponents.add(language);
  }

  private static void addLocatorTypeClass(ChatGptGetContextItem chatGptGetContextItem)
      throws CodeContextOnDemandLocatorException {
    EntityCategory entityCategory;
    try {
      entityCategory =
          EntityCategory.valueOf(
              convertSnakeToPascalCase(chatGptGetContextItem.getEntityCategory()));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid entity category: {}", chatGptGetContextItem.getEntityCategory());
      throw new CodeContextOnDemandLocatorException();
    }
    classNameComponents.add(entityCategory + "Locator");
  }

  private static String getCodeLocatorLanguage(ChatGptGetContextItem chatGptGetContextItem)
      throws CodeContextOnDemandLocatorException {
    String fileExtension;
    try {
      fileExtension = getExtension(chatGptGetContextItem.getFilename());
    } catch (Exception e) {
      throw new CodeContextOnDemandLocatorException();
    }
    return MAP_EXTENSION.get(fileExtension);
  }
}
