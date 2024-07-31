package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.DebugCodeBlocksDataDump;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptThread.KEY_THREAD_ID;

@Slf4j
@Getter
public class ClientCommands extends ClientBase {
    private enum CommandSet {
        REVIEW,
        REVIEW_LAST,
        DIRECTIVES,
        FORGET_THREAD,
        CONFIGURE,
        DUMP_STORED_DATA,
    }
    private enum BaseOptionSet {
        FILTER,
        DEBUG,
        RESET
    }

    private static final Map<String, CommandSet> COMMAND_MAP = Map.of(
            "review", CommandSet.REVIEW,
            "review_last", CommandSet.REVIEW_LAST,
            "directives", CommandSet.DIRECTIVES,
            "forget_thread", CommandSet.FORGET_THREAD,
            "configure", CommandSet.CONFIGURE,
            "dump_stored_data", CommandSet.DUMP_STORED_DATA
    );
    private static final Map<String, BaseOptionSet> BASE_OPTION_MAP = Map.of(
            "filter", BaseOptionSet.FILTER,
            "debug", BaseOptionSet.DEBUG,
            "reset", BaseOptionSet.RESET
    );
    private static final List<CommandSet> REVIEW_COMMANDS = new ArrayList<>(List.of(
            CommandSet.REVIEW,
            CommandSet.REVIEW_LAST
    ));
    private static final List<CommandSet> DEBUG_REQUIRED_COMMANDS = new ArrayList<>(List.of(
            CommandSet.DIRECTIVES,
            CommandSet.CONFIGURE,
            CommandSet.DUMP_STORED_DATA
    ));
    // Option values can be either a sequence of chars enclosed in double quotes or a sequence of non-space chars.
    private static final String OPTION_VALUES = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|\\S+";
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/(" + String.join("|",
            COMMAND_MAP.keySet()) + ")\\b((?:\\s+--\\w+(?:=(?:" + OPTION_VALUES + "))?)+)?");
    private static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(" + OPTION_VALUES + "))?");
    private static final String PREPROCESS_REGEX = "/directives\\s+\"?(.*[^\"])\"?";
    private static final String PREPROCESS_REPLACEMENT = "/configure --directives=\"$1\"";

    private final ChangeSetData changeSetData;
    private final Localizer localizer;

    private PluginDataHandlerProvider pluginDataHandlerProvider;
    private DynamicConfiguration dynamicConfiguration;
    private Map<BaseOptionSet, String> baseOptions;
    private Map<String, String> dynamicOptions;

    public ClientCommands(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        // The `dynamicConfiguration` instance is utilized only from GerritClientComments, not from ClientMessage (from
        // which is used only for cleaning messages).
        if (pluginDataHandlerProvider != null) {
            this.pluginDataHandlerProvider = pluginDataHandlerProvider;
            dynamicConfiguration = new DynamicConfiguration(pluginDataHandlerProvider);
        }
        log.debug("ClientCommands initialized.");
    }

    public boolean parseCommands(String comment) {
        boolean commandFound = false;
        log.debug("Parsing commands from comment: {}", comment);
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(preprocessCommands(comment));
        while (reviewCommandMatcher.find()) {
            baseOptions = new HashMap<>();
            dynamicOptions = new HashMap<>();
            CommandSet command = COMMAND_MAP.get(reviewCommandMatcher.group(1));
            parseOptions(reviewCommandMatcher);
            if (validateCommand(command)) {
                executeCommand(command);
            }
            commandFound = true;
        }
        return commandFound;
    }

    public String removeCommands(String comment) {
        log.debug("Removing commands from comment: {}", comment);
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        return reviewCommandMatcher.replaceAll("");
    }

    private String preprocessCommands(String comment) {
        log.debug("Preprocessing commands: {}", comment);
        return comment.replaceAll(PREPROCESS_REGEX, PREPROCESS_REPLACEMENT);
    }

    private boolean validateCommand(CommandSet command) {
        log.debug("Validating command: {}", command);
        if (!config.getEnableMessageDebugging() && requiresMessageDebugging(command)) {
            changeSetData.setHideChatGptReview(true);
            changeSetData.setReviewSystemMessage(localizer.getText("message.debugging.messages.disabled"));
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

    private void executeCommand(CommandSet command) {
        log.debug("Executing command: {}", command);
        switch (command) {
            case REVIEW, REVIEW_LAST -> commandForceReview(command);
            case FORGET_THREAD -> commandForgetThread();
            case CONFIGURE -> commandDynamicallyConfigure();
            case DUMP_STORED_DATA -> commandDumpStoredData();
        }
    }

    private void commandForceReview(CommandSet command) {
        changeSetData.setForcedReview(true);
        changeSetData.setHideChatGptReview(false);
        changeSetData.setReviewSystemMessage(null);
        if (command == CommandSet.REVIEW_LAST) {
            log.info("Forced review command applied to the last Patch Set");
            changeSetData.setForcedReviewLastPatchSet(true);
        }
        else {
            log.info("Forced review command applied to the entire Change Set");
        }
        if (baseOptions.containsKey(BaseOptionSet.FILTER)) {
            boolean value = Boolean.parseBoolean(baseOptions.get(BaseOptionSet.FILTER));
            log.debug("Option 'replyFilterEnabled' set to {}", value);
            changeSetData.setReplyFilterEnabled(value);
        }
        else if (baseOptions.containsKey(BaseOptionSet.DEBUG)) {
            log.debug("Response Mode set to Debug");
            changeSetData.setDebugReviewMode(true);
            changeSetData.setReplyFilterEnabled(false);
        }
    }

    private void commandForgetThread() {
        PluginDataHandler changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        log.info("Removing thread ID '{}' for Change Set", changeDataHandler.getValue(KEY_THREAD_ID));
        changeDataHandler.removeValue(KEY_THREAD_ID);
        changeSetData.setReviewSystemMessage(localizer.getText("message.command.thread.forget"));
        changeSetData.setHideChatGptReview(true);
    }

    private void commandDynamicallyConfigure() {
        changeSetData.setHideChatGptReview(true);
        boolean modifiedDynamicConfig = false;
        boolean shouldResetDynamicConfig = false;
        if (baseOptions.containsKey(BaseOptionSet.RESET)) {
            shouldResetDynamicConfig = true;
            log.debug("Resetting configuration settings");
        }
        if (!dynamicOptions.isEmpty()) {
            modifiedDynamicConfig = true;
            for (Map.Entry<String, String> dynamicOption : dynamicOptions.entrySet()) {
                String optionKey = dynamicOption.getKey();
                String optionValue = dynamicOption.getValue();
                log.debug("Updating configuration setting '{}' to '{}'", optionKey, optionValue);
                dynamicConfiguration.setConfig(optionKey, optionValue);
            }
        }
        dynamicConfiguration.updateConfiguration(modifiedDynamicConfig, shouldResetDynamicConfig);
    }

    private void commandDumpStoredData() {
        changeSetData.setHideChatGptReview(true);
        DebugCodeBlocksDataDump debugCodeBlocksDataDump = new DebugCodeBlocksDataDump(
                localizer,
                pluginDataHandlerProvider
        );
        changeSetData.setReviewSystemMessage(debugCodeBlocksDataDump.getDataDumpBlock());
    }

    private void parseOptions(Matcher reviewCommandMatcher) {
        log.debug("Parsing options `{}`", reviewCommandMatcher.group(2));
        if (reviewCommandMatcher.group(2) == null) return;
        Matcher reviewOptionsMatcher = OPTIONS_PATTERN.matcher(reviewCommandMatcher.group(2));
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
