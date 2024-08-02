package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public abstract class ClientCommandBase extends ClientBase {
    public enum CommandSet {
        REVIEW,
        REVIEW_LAST,
        DIRECTIVES,
        FORGET_THREAD,
        CONFIGURE,
        DUMP_STORED_DATA,
    }
    public enum BaseOptionSet {
        FILTER,
        DEBUG,
        RESET,
        // `CONFIGURATION_OPTION` is a placeholder option indicating that the associated options must be validated
        // against the Configuration keys.
        CONFIGURATION_OPTION
    }

    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";

    protected static final Pattern COMMAND_PATTERN = Pattern.compile("/(\\w+)\\b((?:\\s+--\\w+(?:=(?:" +
            OPTION_VALUES + "))?)+)?");
    protected static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");

    public ClientCommandBase(Configuration config) {
        super(config);
    }
}
