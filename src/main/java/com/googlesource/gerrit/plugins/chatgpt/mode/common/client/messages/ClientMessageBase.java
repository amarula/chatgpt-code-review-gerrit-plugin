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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public abstract class ClientMessageBase extends ClientBase {
    protected final Pattern botMentionPattern;

    public ClientMessageBase(Configuration config) {
        super(config);
        botMentionPattern = getBotMentionPattern();
        log.debug("ClientMessageBase initialized with bot mention pattern: {}", botMentionPattern);
    }

    private Pattern getBotMentionPattern() {
        String emailRegex = "^(?!>).*?(?:@" + getUserNameOrEmail() + ")\\b";
        log.debug("Generated bot mention pattern: {}", emailRegex);
        return Pattern.compile(emailRegex, Pattern.MULTILINE);
    }

    private String getUserNameOrEmail() {
        String escapedUserName = Pattern.quote(config.getGerritUserName());
        String userEmail = config.getGerritUserEmail();
        if (userEmail.isBlank()) {
            return  escapedUserName;
        }
        return escapedUserName + "|" + Pattern.quote(userEmail);
    }
}
