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

package com.googlesource.gerrit.plugins.reviewai.data;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;

@Singleton
@Slf4j
public class PluginDataHandler {
  private final Path configFile;
  private final Properties configProperties = new Properties();

  @Inject
  public PluginDataHandler(Path configFilePath) {
    this.configFile = configFilePath;
    try {
      log.debug("Loading or creating properties file at: {}", configFilePath);
      loadOrCreateProperties();
    } catch (IOException e) {
      log.error("Failed to load or create properties", e);
      throw new RuntimeException(e);
    }
  }

  public synchronized void setValue(String key, String value) {
    log.debug("Setting value for key: {} with value: {}", key, value);
    configProperties.setProperty(key, value);
    storeProperties();
  }

  public synchronized void setJsonValue(String key, Object value) {
    log.debug("Setting JSON value for key: {}", key);
    setValue(key, getGson().toJson(value));
  }

  public String getValue(String key) {
    log.debug("Getting value for key: {}", key);
    return configProperties.getProperty(key);
  }

  public <T> List<T> getJsonArrayValue(String key, Class<T> clazz) {
    Type typeOfArray = TypeToken.getParameterized(List.class, clazz).getType();
    return getJsonValue(key, typeOfArray);
  }

  public <T> Map<String, T> getJsonObjectValue(String key, Class<T> clazz) {
    Type typeOfMap = TypeToken.getParameterized(Map.class, String.class, clazz).getType();
    return getJsonValue(key, typeOfMap);
  }

  public <T> T getJsonValue(String key, Type type) {
    log.debug("Getting JSON value for key: {}", key);
    String value = getValue(key);
    if (value == null || value.isEmpty()) {
      log.debug("No value found for key: {}", key);
      return null;
    }
    return getGson().fromJson(value, type);
  }

  public Map<String, String> getAllValues() {
    log.debug("Getting all properties");
    Map<String, String> allProperties = new HashMap<>();
    for (String key : configProperties.stringPropertyNames()) {
      allProperties.put(key, configProperties.getProperty(key));
    }
    return allProperties;
  }

  public synchronized <T> void appendJsonValue(String key, T value, Class<T> clazz) {
    log.debug("Updating JSON value for key: {}", key);
    List<T> jsonProperty = getJsonArrayValue(key, clazz);
    if (jsonProperty == null) {
      jsonProperty = new ArrayList<>();
    }
    jsonProperty.add(value);
    setJsonValue(key, jsonProperty);
  }

  public synchronized void removeValue(String key) {
    log.debug("Removing value for key: {}", key);
    if (configProperties.containsKey(key)) {
      configProperties.remove(key);
      storeProperties();
    }
  }

  public synchronized void destroy() {
    log.debug("Destroying configuration file at: {}", configFile);
    try {
      Files.deleteIfExists(configFile);
    } catch (IOException e) {
      log.error("Failed to delete the config file: " + configFile, e);
      throw new RuntimeException("Failed to delete the config file: " + configFile, e);
    }
  }

  private void storeProperties() {
    log.debug("Storing properties to file: {}", configFile);
    try (var output = Files.newOutputStream(configFile)) {
      configProperties.store(output, null);
    } catch (IOException e) {
      log.error("Failed to store properties", e);
      throw new RuntimeException(e);
    }
  }

  private void loadOrCreateProperties() throws IOException {
    log.debug("Checking existence of the configuration file: {}", configFile);
    if (Files.notExists(configFile)) {
      log.debug("Configuration file not found, creating new one at: {}", configFile);
      Files.createFile(configFile);
    } else {
      log.debug("Loading properties from file: {}", configFile);
      try (var input = Files.newInputStream(configFile)) {
        configProperties.load(input);
      }
    }
  }
}
