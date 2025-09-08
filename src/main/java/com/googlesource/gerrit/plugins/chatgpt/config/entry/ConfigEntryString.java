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

package com.googlesource.gerrit.plugins.chatgpt.config.entry;

import com.google.gerrit.server.config.PluginConfig;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.config.entry.IConfigEntry;

import java.util.Map;

public class ConfigEntryString extends ConfigEntryBase implements IConfigEntry {

  public ConfigEntryString(String key) {
    super(key);
  }

  public void setCurrentConfigValue(Map<String, Object> configValues, PluginConfig projectConfig) {
    configValues.put(key, projectConfig.getString(key));
  }

  public void setDynamicConfigValue(Map<String, Object> configValues, String value) {
    configValues.put(key, value);
  }

  public void setMergedConfigValue(PluginConfig.Update configUpdater, Object value) {
    configUpdater.setString(key, (String) value);
  }
}
