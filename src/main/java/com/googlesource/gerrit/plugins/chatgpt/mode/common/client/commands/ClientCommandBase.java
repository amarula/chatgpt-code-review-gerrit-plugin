package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.google.common.collect.ImmutableBiMap;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public abstract class ClientCommandBase extends ClientBase {
    public enum CommandSet {
        MESSAGE,
        REVIEW,
        REVIEW_LAST,
        DIRECTIVES,
        FORGET_THREAD,
        CONFIGURE,
        DUMP_CONFIG,
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

    protected static final ImmutableBiMap<String, CommandSet> COMMAND_MAP = ImmutableBiMap.of(
            "message", CommandSet.MESSAGE,
            "review", CommandSet.REVIEW,
            "review_last", CommandSet.REVIEW_LAST,
            "directives", CommandSet.DIRECTIVES,
            "forget_thread", CommandSet.FORGET_THREAD,
            "configure", CommandSet.CONFIGURE,
            "dump_config", CommandSet.DUMP_CONFIG,
            "dump_stored_data", CommandSet.DUMP_STORED_DATA
    );

    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";

    protected static final Pattern MESSAGE_COMMAND_PATTERN = Pattern.compile("\\s*/" +
            COMMAND_MAP.inverse().get(CommandSet.MESSAGE) + "\\b(.*)$");
    protected static final Pattern COMMAND_PATTERN = Pattern.compile("/(\\w+)\\b((?:\\s+--\\w+(?:=(?:" +
            OPTION_VALUES + "))?)+)?");
    protected static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");

    public ClientCommandBase(Configuration config) {
        super(config);
    }
}
