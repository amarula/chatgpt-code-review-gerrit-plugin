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

package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.util.List;

public class ModuleUtils {
  public static final String MODULE_SEPARATOR = ".";

  public static String convertDotNotationToPath(String dotNotated) {
    return dotNotated.replace(MODULE_SEPARATOR.charAt(0), '/');
  }

  public static String getSimpleName(String moduleName) {
    int lastDotIndex = moduleName.lastIndexOf(MODULE_SEPARATOR);
    if (lastDotIndex <= 0 || lastDotIndex == moduleName.length() - 1) {
      return moduleName;
    }
    return moduleName.substring(lastDotIndex + 1);
  }

  public static String joinComponents(List<String> components) {
    return String.join(MODULE_SEPARATOR, components);
  }
}
