package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.Directives;
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
    private enum COMMAND_SET {
        REVIEW,
        REVIEW_LAST,
        DIRECTIVE,
        CONFIGURE
    }
    private enum REVIEW_OPTION_SET {
        FILTER,
        DEBUG
    }
    private enum CONFIGURE_OPTION_SET {
        RESET
    }

    private static final Map<String, COMMAND_SET> COMMAND_MAP = Map.of(
            "review", COMMAND_SET.REVIEW,
            "review_last", COMMAND_SET.REVIEW_LAST,
            "directive", COMMAND_SET.DIRECTIVE,
            "configure", COMMAND_SET.CONFIGURE
    );
    private static final Map<String, REVIEW_OPTION_SET> REVIEW_OPTION_MAP = Map.of(
            "filter", REVIEW_OPTION_SET.FILTER,
            "debug", REVIEW_OPTION_SET.DEBUG
    );
    private static final List<COMMAND_SET> REVIEW_COMMANDS = new ArrayList<>(List.of(
            COMMAND_SET.REVIEW,
            COMMAND_SET.REVIEW_LAST
    ));
    private static final List<COMMAND_SET> HISTORY_COMMANDS = new ArrayList<>(List.of(
            COMMAND_SET.DIRECTIVE
    ));
    private static final Map<String, CONFIGURE_OPTION_SET> CONFIGURE_OPTION_MAP = Map.of(
            "reset", CONFIGURE_OPTION_SET.RESET
    );
    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/(" + String.join("|",
            COMMAND_MAP.keySet()) + ")\\b((?:\\s+--\\w+(?:=(?:" + OPTION_VALUES + "))?)+)?");
    private static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");

    private final ChangeSetData changeSetData;
    private final Directives directives;
    private final Localizer localizer;

    private DynamicConfiguration dynamicConfiguration;
    private boolean containingHistoryCommand;
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
        directives = new Directives(changeSetData);
        // The `dynamicConfiguration` instance is utilized only for parsing current client messages, not the history
        if (pluginDataHandlerProvider != null) {
            dynamicConfiguration = new DynamicConfiguration(pluginDataHandlerProvider);
        }
        containingHistoryCommand = false;
        modifiedDynamicConfig = false;
        shouldResetDynamicConfig = false;
    }

    public boolean parseCommands(String comment, boolean isNotHistory) {
        boolean commandFound = false;
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        while (reviewCommandMatcher.find()) {
            COMMAND_SET command = COMMAND_MAP.get(reviewCommandMatcher.group(1));
            parseOptions(command, reviewCommandMatcher, isNotHistory);
            parseCommand(command, comment, isNotHistory);
            commandFound = true;
        }
        return commandFound;
    }

    public String parseRemoveCommands(String comment) {
        if (parseCommands(comment, false)) {
            return removeCommands(comment);
        }
        return comment;
    }

    private String removeCommands(String comment) {
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        return reviewCommandMatcher.replaceAll("");
    }

    private void parseCommand(COMMAND_SET command, String comment, boolean isNotHistory) {
        if (isNotHistory) {
            if (REVIEW_COMMANDS.contains(command)) {
                changeSetData.setForcedReview(true);
                if (command == COMMAND_SET.REVIEW_LAST) {
                    log.info("Forced review command applied to the last Patch Set");
                    changeSetData.setForcedReviewLastPatchSet(true);
                }
                else {
                    log.info("Forced review command applied to the entire Change Set");
                }
            }
            else if (command == COMMAND_SET.CONFIGURE) {
                if (config.getEnableMessageDebugging()) {
                    changeSetData.setHideChatGptReview(true);
                    changeSetData.setForceDisplaySystemMessage(true);
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
        if (HISTORY_COMMANDS.contains(command)) {
            containingHistoryCommand = true;
            if (command == COMMAND_SET.DIRECTIVE) {
                directives.addDirective(removeCommands(comment));
            }
        }
    }

    private void parseOptions(COMMAND_SET command, Matcher reviewCommandMatcher, boolean isNotHistory) {
        // Command options need to be parsed only when processing the current message, not the message history
        if (reviewCommandMatcher.group(2) == null || !isNotHistory) return;
        Matcher reviewOptionsMatcher = OPTIONS_PATTERN.matcher(reviewCommandMatcher.group(2));
        while (reviewOptionsMatcher.find()) {
            parseSingleOption(command, reviewOptionsMatcher);
        }
    }

    private void parseSingleOption(COMMAND_SET command, Matcher reviewOptionsMatcher) {
        String optionKey = reviewOptionsMatcher.group(1);
        String optionValue = Optional.ofNullable(reviewOptionsMatcher.group(2))
                .map(val -> val.replaceAll("^\"(.*)\"$", "$1"))
                .orElse("");
        if (REVIEW_COMMANDS.contains(command)) {
            switch (REVIEW_OPTION_MAP.get(optionKey)) {
                case FILTER:
                    boolean value = Boolean.parseBoolean(optionValue);
                    log.debug("Option 'replyFilterEnabled' set to {}", value);
                    changeSetData.setReplyFilterEnabled(value);
                    break;
                case DEBUG:
                    if (config.getEnableMessageDebugging()) {
                        log.debug("Response Mode set to Debug");
                        changeSetData.setDebugReviewMode(true);
                        changeSetData.setReplyFilterEnabled(false);
                    } else {
                        changeSetData.setReviewSystemMessage(localizer.getText(
                                "message.debugging.review.disabled"
                        ));
                        log.debug("Unable to set Response Mode to Debug: `enableMessageDebugging` config " +
                                "must be set to true");
                    }
                    break;
            }
        } else if (command == COMMAND_SET.CONFIGURE && config.getEnableMessageDebugging()) {
            if (CONFIGURE_OPTION_MAP.get(optionKey) == CONFIGURE_OPTION_SET.RESET) {
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
