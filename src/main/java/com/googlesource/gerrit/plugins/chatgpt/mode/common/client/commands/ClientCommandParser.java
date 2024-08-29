package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;

@Slf4j
public class ClientCommandParser extends ClientCommandBase {
    private static final Map<String, BaseOptionSet> BASE_OPTION_MAP = Map.of(
            "filter", BaseOptionSet.FILTER,
            "debug", BaseOptionSet.DEBUG,
            "reset", BaseOptionSet.RESET,
            "remove", BaseOptionSet.REMOVE
    );
    private static final Map<CommandSet, List<BaseOptionSet>> COMMAND_VALID_OPTIONS_MAP = Map.of(
            CommandSet.REVIEW, List.of(BaseOptionSet.FILTER, BaseOptionSet.DEBUG),
            CommandSet.REVIEW_LAST, List.of(BaseOptionSet.FILTER, BaseOptionSet.DEBUG),
            CommandSet.CONFIGURE, List.of(BaseOptionSet.RESET, BaseOptionSet.CONFIGURATION_OPTION),
            CommandSet.DIRECTIVES, List.of(BaseOptionSet.RESET, BaseOptionSet.REMOVE)
    );
    private static final List<CommandSet> REVIEW_COMMANDS = new ArrayList<>(List.of(
            CommandSet.REVIEW,
            CommandSet.REVIEW_LAST
    ));
    private static final List<CommandSet> DEBUG_REQUIRED_COMMANDS = new ArrayList<>(List.of(
            CommandSet.DIRECTIVES,
            CommandSet.CONFIGURE,
            CommandSet.DUMP_CONFIG,
            CommandSet.DUMP_STORED_DATA,
            CommandSet.UPLOAD_CODEBASE
    ));

    private final ChangeSetData changeSetData;
    private final Localizer localizer;
    private final ClientCommandExecutor clientCommandExecutor;

    private String comment;
    private Map<BaseOptionSet, String> baseOptions;
    private Map<String, String> dynamicOptions;

    public ClientCommandParser(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        this.clientCommandExecutor = new ClientCommandExecutor(
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider,
                localizer
        );
        log.debug("ClientCommandParser initialized.");
    }

    public boolean parseCommands(String comment) {
        this.comment = comment;
        boolean commandFound = false;
        log.debug("Parsing commands from comment: {}", comment);
        if (parseMessageCommand(comment)) {
            log.debug("Message command detected: parsing complete.");
            return false;
        }
        Matcher commandMatcher = COMMAND_PATTERN.matcher(comment);
        changeSetData.setHideChatGptReview(true);
        while (commandMatcher.find()) {
            if (!parseSingleCommand(comment, commandMatcher)) {
                return false;
            }
            commandFound = true;
        }
        if (!changeSetData.getForcedReview()) {
            changeSetData.setHideChatGptReview(false);
        }
        return commandFound;
    }

    private boolean parseMessageCommand(String comment) {
        Matcher messageCommandMatcher = MESSAGE_COMMAND_PATTERN.matcher(comment);
        return messageCommandMatcher.find();
    }

    private boolean parseSingleCommand(String comment, Matcher commandMatcher) {
        baseOptions = new HashMap<>();
        dynamicOptions = new HashMap<>();
        CommandSet command = COMMAND_MAP.get(commandMatcher.group(1));
        if (command == null) {
            changeSetData.setReviewSystemMessage(String.format(localizer.getText("message.command.unknown"),
                    comment));
            log.info("Unknown command in comment `{}`", comment);
            return false;
        }
        parseOptions(commandMatcher);
        if (validateCommand(command)) {
            clientCommandExecutor.executeCommand(
                    command,
                    baseOptions,
                    dynamicOptions,
                    comment.substring(commandMatcher.end())
            );
            clientCommandExecutor.postExecuteCommand();
        }
        else {
            log.info("Command in comment `{}` not validated", comment);
        }
        return true;
    }

    private boolean validateCommand(CommandSet command) {
        log.debug("Validating command: {}", command);
        if (optionsMismatch(command)) {
            changeSetData.setReviewSystemMessage(String.format(localizer.getText("message.command.options.mismatch"),
                    comment));
            log.debug("Option mismatch for command `{}`. Comment: {}", command, comment);
            return false;
        }
        if (!config.getEnableMessageDebugging() && requiresMessageDebugging(command)) {
            changeSetData.setReviewSystemMessage(localizer.getText("message.command.debugging.messages.disabled"));
            log.debug("Command `{}` not validated: `enableMessageDebugging` config must be set to true", command);
            return false;
        }
        log.debug("Command `{}` validated", command);
        return true;
    }

    private boolean requiresMessageDebugging(CommandSet command) {
        return DEBUG_REQUIRED_COMMANDS.contains(command) ||
                REVIEW_COMMANDS.contains(command) && baseOptions.containsKey(BaseOptionSet.DEBUG);
    }

    private boolean optionsMismatch(CommandSet command) {
        log.debug("Validating options for command: {}", command);
        List<BaseOptionSet> commandOptions = COMMAND_VALID_OPTIONS_MAP.get(command);
        if (!baseOptions.isEmpty() && (
                commandOptions == null || !(new HashSet<>(commandOptions).containsAll(baseOptions.keySet()))
        )) {
            log.debug("Options non valid for command `{}`: {}", command, baseOptions);
            return true;
        }
        if (!dynamicOptions.isEmpty() &&(
                !commandOptions.contains(BaseOptionSet.CONFIGURATION_OPTION) || configurationOptionsMismatch()
        )) {
            log.debug("Configuration options non valid for command `{}`: {}", command, dynamicOptions);
            return true;
        }
        return false;
    }

    private boolean configurationOptionsMismatch() {
        log.debug("Checking for mismatches in configuration options");
        for (String key : dynamicOptions.keySet()) {
            if (!config.isDefinedKey(key)) {
                log.debug("Configuration option mismatch found for key `{}`", key);
                return true;
            }
        }
        return false;
    }

    private void parseOptions(Matcher commandMatcher) {
        log.debug("Parsing options `{}`", commandMatcher.group(2));
        if (commandMatcher.group(2) == null) return;
        Matcher reviewOptionsMatcher = OPTIONS_PATTERN.matcher(commandMatcher.group(2));
        while (reviewOptionsMatcher.find()) {
            parseSingleOption(reviewOptionsMatcher);
        }
    }

    private void parseSingleOption(Matcher reviewOptionsMatcher) {
        String optionKey = reviewOptionsMatcher.group(1);
        String optionValue = Optional.ofNullable(reviewOptionsMatcher.group(2))
                .map(TextUtils::unwrapDeSlashQuotes)
                .orElse("");
        log.debug("Parsed option - Key: {} - Value: {}", optionKey, optionValue);
        if (BASE_OPTION_MAP.containsKey(optionKey)) {
            baseOptions.put(BASE_OPTION_MAP.get(optionKey), optionValue);
        }
        else {
            dynamicOptions.put(optionKey, optionValue);
        }
    }
}
