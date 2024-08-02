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
