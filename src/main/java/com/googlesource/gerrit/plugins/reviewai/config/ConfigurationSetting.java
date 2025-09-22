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

package com.googlesource.gerrit.plugins.reviewai.config;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a single configuration setting.
 *
 * The class encapsulates the configuration key, default value, and the lookup strategy required
 * to resolve the effective value from the Gerrit configuration scopes.
 */
public final class ConfigurationSetting<T> {
  @FunctionalInterface
  public interface SettingReader<T> {
    T read(ConfigCore configCore, ConfigurationSetting<T> setting);
  }

  private final String key;
  private final T defaultValue;
  private final SettingReader<T> reader;

  private ConfigurationSetting(String key, T defaultValue, SettingReader<T> reader) {
    this.key = Objects.requireNonNull(key, "key");
    this.defaultValue = defaultValue;
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  public static <T> ConfigurationSetting<T> of(
      String key, T defaultValue, SettingReader<T> reader) {
    return new ConfigurationSetting<>(key, defaultValue, reader);
  }

  public static ConfigurationSetting<String> requiredString(String key) {
    return new ConfigurationSetting<>(
        key, null, (core, setting) -> core.getValidatedOrThrow(setting.key()));
  }

  public static ConfigurationSetting<String> string(String key, String defaultValue) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    return new ConfigurationSetting<>(
        key, defaultValue, (core, setting) -> core.getString(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<Boolean> bool(String key, boolean defaultValue) {
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) -> core.getBoolean(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<Integer> integer(String key, int defaultValue) {
    return new ConfigurationSetting<>(
        key, defaultValue, (core, setting) -> core.getInt(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<Double> decimal(String key, double defaultValue) {
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) -> core.getDouble(setting.key(), setting.defaultValue()));
  }

  public static <E extends Enum<E>> ConfigurationSetting<E> enumSetting(
      String key, E defaultValue, Class<E> enumClass) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    Objects.requireNonNull(enumClass, "enumClass");
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) -> core.getEnum(setting.key(), setting.defaultValue(), enumClass));
  }

  public static ConfigurationSetting<Boolean> globalBoolean(String key, boolean defaultValue) {
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) ->
            core.getGlobalConfig().getBoolean(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<String> globalString(String key, String defaultValue) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) ->
            core.getGlobalConfig().getString(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<Integer> globalInteger(String key, int defaultValue) {
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) ->
            core.getGlobalConfig().getInt(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<Boolean> projectBoolean(String key, boolean defaultValue) {
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) ->
            core.getProjectConfig().getBoolean(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<String> projectString(String key, String defaultValue) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) ->
            core.getProjectConfig().getString(setting.key(), setting.defaultValue()));
  }

  public static ConfigurationSetting<List<String>> stringList(
      String key, List<String> defaultValue) {
    Objects.requireNonNull(defaultValue, "defaultValue");
    return new ConfigurationSetting<>(
        key,
        defaultValue,
        (core, setting) -> core.splitListIntoItems(setting.key(), setting.defaultValue()));
  }

  public T read(ConfigCore configCore) {
    return reader.read(configCore, this);
  }

  public String key() {
    return key;
  }

  public T defaultValue() {
    return defaultValue;
  }
}
