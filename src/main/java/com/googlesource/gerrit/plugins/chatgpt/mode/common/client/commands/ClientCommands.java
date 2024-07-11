package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class ClientCommands extends ClientBase {
    private enum CommandSet {
        REVIEW,
        REVIEW_LAST,
        DIRECTIVES,
        CONFIGURE
    }
    private enum ReviewOptionSet {
        FILTER,
        DEBUG
    }
    private enum ConfigureOptionSet {
        RESET
    }

    private static final Map<String, CommandSet> COMMAND_MAP = Map.of(
            "review", CommandSet.REVIEW,
            "review_last", CommandSet.REVIEW_LAST,
            "directives", CommandSet.DIRECTIVES,
            "configure", CommandSet.CONFIGURE
    );
    private static final Map<String, ReviewOptionSet> REVIEW_OPTION_MAP = Map.of(
            "filter", ReviewOptionSet.FILTER,
            "debug", ReviewOptionSet.DEBUG
    );
    private static final List<CommandSet> REVIEW_COMMANDS = new ArrayList<>(List.of(
            CommandSet.REVIEW,
            CommandSet.REVIEW_LAST
    ));
    private static final Map<String, ConfigureOptionSet> CONFIGURE_OPTION_MAP = Map.of(
            "reset", ConfigureOptionSet.RESET
    );
    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/(" + String.join("|",
            COMMAND_MAP.keySet()) + ")\\b((?:\\s+--\\w+(?:=(?:" + OPTION_VALUES + "))?)+)?");
    private static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");
    private static final String PREPROCESS_REGEX = "/directives\\s+\"?(.*[^\"])\"?";
    private static final String PREPROCESS_REPLACEMENT = "/configure --directives=\"$1\"";

    private final ChangeSetData changeSetData;
    private final Localizer localizer;

    private DynamicConfiguration dynamicConfiguration;
    private boolean modifiedDynamicConfig;
    private boolean shouldResetDynamicConfig;

    public ClientCommands(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        // The `dynamicConfiguration` instance is utilized only for parsing current client messages, not the history
        if (pluginDataHandlerProvider != null) {
            dynamicConfiguration = new DynamicConfiguration(pluginDataHandlerProvider);
        }
        modifiedDynamicConfig = false;
        shouldResetDynamicConfig = false;
    }

    public boolean parseCommands(String comment) {
        boolean commandFound = false;
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(preprocessCommands(comment));
        while (reviewCommandMatcher.find()) {
            CommandSet command = COMMAND_MAP.get(reviewCommandMatcher.group(1));
            parseOptions(command, reviewCommandMatcher);
            parseCommand(command);
            commandFound = true;
        }
        return commandFound;
    }

    public String removeCommands(String comment) {
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        return reviewCommandMatcher.replaceAll("");
    }

    private String preprocessCommands(String comment) {
        return comment.replaceAll(PREPROCESS_REGEX, PREPROCESS_REPLACEMENT);
    }

    private void parseCommand(CommandSet command) {
        switch (command) {
            case REVIEW, REVIEW_LAST -> {
                changeSetData.setForcedReview(true);
                if (command == CommandSet.REVIEW_LAST) {
                    log.info("Forced review command applied to the last Patch Set");
                    changeSetData.setForcedReviewLastPatchSet(true);
                }
                else {
                    log.info("Forced review command applied to the entire Change Set");
                }
            }
            case CONFIGURE -> {
                if (config.getEnableMessageDebugging()) {
                    changeSetData.setHideChatGptReview(true);
                    dynamicConfiguration.updateConfiguration(modifiedDynamicConfig, shouldResetDynamicConfig);
                }
                else {
                    changeSetData.setReviewSystemMessage(localizer.getText(
                            "message.configure.from.messages.disabled"
                    ));
                    log.debug("Unable to change configuration from messages: `enableMessageDebugging` config must" +
                            "be set to true");
                }
            }
        }
    }

    private void parseOptions(CommandSet command, Matcher reviewCommandMatcher) {
        if (reviewCommandMatcher.group(2) == null) return;
        Matcher reviewOptionsMatcher = OPTIONS_PATTERN.matcher(reviewCommandMatcher.group(2));
        while (reviewOptionsMatcher.find()) {
            parseSingleOption(command, reviewOptionsMatcher);
        }
    }

    private void parseSingleOption(CommandSet command, Matcher reviewOptionsMatcher) {
        String optionKey = reviewOptionsMatcher.group(1);
        String optionValue = Optional.ofNullable(reviewOptionsMatcher.group(2))
                .map(val -> val.replaceAll("^\"(.*)\"$", "$1"))
                .orElse("");
        if (REVIEW_COMMANDS.contains(command)) {
            switch (REVIEW_OPTION_MAP.get(optionKey)) {
                case FILTER -> {
                    boolean value = Boolean.parseBoolean(optionValue);
                    log.debug("Option 'replyFilterEnabled' set to {}", value);
                    changeSetData.setReplyFilterEnabled(value);
                }
                case DEBUG -> {
                    if (config.getEnableMessageDebugging()) {
                        log.debug("Response Mode set to Debug");
                        changeSetData.setDebugReviewMode(true);
                        changeSetData.setReplyFilterEnabled(false);
                    }
                    else {
                        changeSetData.setReviewSystemMessage(localizer.getText(
                                "message.debugging.review.disabled"
                        ));
                        log.debug("Unable to set Response Mode to Debug: `enableMessageDebugging` config " +
                                "must be set to true");
                    }
                }
            }
        }
        else if (command == CommandSet.CONFIGURE && config.getEnableMessageDebugging()) {
            if (CONFIGURE_OPTION_MAP.get(optionKey) == ConfigureOptionSet.RESET) {
                shouldResetDynamicConfig = true;
                log.debug("Resetting configuration settings");
            }
            else {
                modifiedDynamicConfig = true;
                log.debug("Updating configuration setting '{}' to '{}'", optionKey, optionValue);
                dynamicConfiguration.setConfig(optionKey, optionValue);
            }
        }
    }
}
