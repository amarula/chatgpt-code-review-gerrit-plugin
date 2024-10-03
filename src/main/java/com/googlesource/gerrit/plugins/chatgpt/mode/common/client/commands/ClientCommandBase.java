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
        SHOW,
        UPLOAD_CODEBASE
    }
    public enum BaseOptionSet {
        FILTER,
        DEBUG,
        RESET,
        REMOVE,
        // `CONFIGURATION_OPTION` is a placeholder option indicating that the associated options must be validated
        // against the Configuration keys.
        CONFIGURATION_OPTION,
        CONFIG,
        LOCAL_DATA,
        PROMPTS
    }

    protected static final ImmutableBiMap<String, CommandSet> COMMAND_MAP = ImmutableBiMap.of(
            "message", CommandSet.MESSAGE,
            "review", CommandSet.REVIEW,
            "review_last", CommandSet.REVIEW_LAST,
            "directives", CommandSet.DIRECTIVES,
            "forget_thread", CommandSet.FORGET_THREAD,
            "configure", CommandSet.CONFIGURE,
            "show", CommandSet.SHOW,
            "upload_codebase", CommandSet.UPLOAD_CODEBASE
    );
    private static final ImmutableBiMap<CommandSet, String> COMMAND_MAP_INVERSE = COMMAND_MAP.inverse();

    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";

    protected static final Pattern MESSAGE_COMMAND_PATTERN = Pattern.compile("\\s*/" +
            COMMAND_MAP_INVERSE.get(CommandSet.MESSAGE) + "\\b(.*)$");
    protected static final Pattern DIRECTIVE_COMMAND_PATTERN = Pattern.compile("\\s*/" +
            COMMAND_MAP_INVERSE.get(CommandSet.DIRECTIVES) + "\\b.*$");
    protected static final Pattern COMMAND_PATTERN = Pattern.compile("/(\\w+)\\b((?:\\s+--\\w+(?:=(?:" +
            OPTION_VALUES + "))?)+)?");
    protected static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");

    public ClientCommandBase(Configuration config) {
        super(config);
    }
}
