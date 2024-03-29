package com.googlesource.gerrit.plugins.chatgpt.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.commands.ClientCommands;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
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

    @Getter
    private String message;

    public ClientMessage(Configuration config, GerritChange change) {
        super(config);
        botMentionPattern = getBotMentionPattern();
        clientCommands = new ClientCommands(change);
    }

    public ClientMessage(Configuration config, GerritChange change, String message) {
        this(config, change);
        this.message = message;
    }

    public boolean isBotAddressed(String message) {
        log.debug("Processing comment: {}", message);
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
        message = MESSAGE_HEADING_PATTERN.matcher(message).replaceAll("");
        return this;
    }

    public ClientMessage removeMentions() {
        message = botMentionPattern.matcher(message).replaceAll("").trim();
        return this;
    }

    public ClientMessage parseRemoveCommands() {
        message = clientCommands.parseRemoveCommands(message);
        return this;
    }

    public ClientMessage removeDebugMessages() {
        message = DebugMessages.removeDebugMessages(message);
        return this;
    }

    public boolean isContainingHistoryCommand() {
        return clientCommands.isContainingHistoryCommand();
    }

    public boolean parseCommands(String comment, boolean isNotHistory) {
        return clientCommands.parseCommands(comment, isNotHistory);
    }

    public void processHistoryCommand() {
        clientCommands.getDirectives().copyDirectiveToSettings();
    }

    private Pattern getBotMentionPattern() {
        String escapedUserName = Pattern.quote(config.getGerritUserName());
        String emailRegex = "@" + escapedUserName + "(?:@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})?\\b";
        return Pattern.compile(emailRegex);
    }

}
