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

package com.googlesource.gerrit.plugins.chatgpt.errors;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j
public class ErrorMessageHandler {
    private final Configuration config;
    private final Localizer localizer;

    public ErrorMessageHandler(Configuration config, Localizer localizer) {
        this.config = config;
        this.localizer = localizer;
    }

    public void updateErrorMessages(List<String> messages) {
        Set<String> unknownEnumSettings = config.getUnknownEnumSettings();
        log.debug("Updating error messages with: {}", unknownEnumSettings);
        if (unknownEnumSettings.isEmpty()) {
            return;
        }
        messages.addAll(unknownEnumSettings.stream()
                .map(m -> String.format(localizer.getText("message.config.unknown.enum.error"), m))
                .toList()
        );
        log.debug("Updated error messages: {}", messages);
    }
}
