package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands.ClientCommands;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ClientMessage extends ClientBase {
    private static final Pattern MESSAGE_HEADING_PATTERN = Pattern.compile(
            "^(?:Patch Set \\d+:[^\\n]*\\s+(?:\\(\\d+ comments?\\)\\s*)?)+");

    private final Pattern botMentionPattern;
    private final ClientCommands clientCommands;
    private final DebugCodeBlocksReview debugCodeBlocksReview;
    private final DebugCodeBlocksDynamicSettings debugCodeBlocksDynamicSettings;

    @Getter
    private String message;

    public ClientMessage(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        botMentionPattern = getBotMentionPattern();
        clientCommands = new ClientCommands(config, changeSetData, pluginDataHandlerProvider, localizer);
        debugCodeBlocksReview = new DebugCodeBlocksReview(localizer);
        debugCodeBlocksDynamicSettings = new DebugCodeBlocksDynamicSettings(localizer);
        log.debug("ClientMessage initialized with bot mention pattern: {}", botMentionPattern);
    }

    public ClientMessage(Configuration config, ChangeSetData changeSetData, String message, Localizer localizer) {
        this(config, changeSetData, (PluginDataHandlerProvider) null, localizer);
        this.message = message;
        log.debug("ClientMessage initialized with specific message: {}", message);
    }

    public boolean isBotAddressed(String message) {
        log.debug("Checking if message addresses the bot: {}", message);
        Matcher userMatcher = botMentionPattern.matcher(message);
        if (!userMatcher.find()) {
            log.debug("Skipping action since the comment does not mention the ChatGPT bot." +
                            " Expected bot name in comment: {}, Actual comment text: {}",
                    config.getGerritUserName(), message);
            return false;
        }
        return true;
    }

    public ClientMessage removeHeadings() {
        log.debug("Removing headings from message.");
        message = MESSAGE_HEADING_PATTERN.matcher(message).replaceAll("");
        log.debug("Message after removing headings: {}", message);
        return this;
    }

    public ClientMessage removeMentions() {
        log.debug("Removing bot mentions from message.");
        message = botMentionPattern.matcher(message).replaceAll("").trim();
        log.debug("Message after removing mentions: {}", message);
        return this;
    }

    public ClientMessage removeCommands() {
        log.debug("Removing commands from message.");
        message = clientCommands.removeCommands(message);
        log.debug("Message after removing commands: {}", message);
        return this;
    }

    public ClientMessage removeDebugCodeBlocksReview() {
        log.debug("Removing debug code blocks for review.");
        message = debugCodeBlocksReview.removeDebugCodeBlocks(message);
        log.debug("Message after removing debug code blocks: {}", message);
        return this;
    }

    public ClientMessage removeDebugCodeBlocksDynamicSettings() {
        log.debug("Removing debug code blocks for dynamic settings.");
        message = debugCodeBlocksDynamicSettings.removeDebugCodeBlocks(message);
        log.debug("Message after removing dynamic settings debug code blocks: {}", message);
        return this;
    }

    public boolean parseCommands(String comment) {
        log.debug("Parsing commands from comment: {}", comment);
        return clientCommands.parseCommands(comment);
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
