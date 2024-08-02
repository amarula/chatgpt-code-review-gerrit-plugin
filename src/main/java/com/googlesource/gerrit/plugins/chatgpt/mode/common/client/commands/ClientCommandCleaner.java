package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

@Slf4j
public class ClientCommandCleaner extends ClientCommandBase {
    public ClientCommandCleaner(Configuration config) {
        super(config);
    }

    public String removeCommands(String comment) {
        log.debug("Removing commands from comment: {}", comment);

        Matcher messageCommandMatcher = MESSAGE_COMMAND_PATTERN.matcher(comment);
        if (messageCommandMatcher.find()) {
            return messageCommandMatcher.replaceAll("$1");
        }
        Matcher commandMatcher = COMMAND_PATTERN.matcher(comment);
        return commandMatcher.replaceAll("");
    }
}
