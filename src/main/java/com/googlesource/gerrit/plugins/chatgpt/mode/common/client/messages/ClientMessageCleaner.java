package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands.ClientCommandCleaner;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksCleaner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_DEFAULT_MESSAGE_COMMENTS;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_DEFAULT_MESSAGE_PATCH_SET;

@Slf4j
public class ClientMessageCleaner extends ClientMessageBase {
    private final Pattern messageHeadingPattern;
    private final DebugCodeBlocksCleaner debugCodeBlocksCleaner;
    private final ClientCommandCleaner clientCommandCleaner;

    @Getter
    protected String message;

    public ClientMessageCleaner(Configuration config, String message, Localizer localizer) {
        super(config);
        this.message = message;
        debugCodeBlocksCleaner = new DebugCodeBlocksCleaner(localizer);
        clientCommandCleaner = new ClientCommandCleaner(config);
        messageHeadingPattern = Pattern.compile(
                localizer.getText("system.message.prefix") + ".*$|^" +
                        GERRIT_DEFAULT_MESSAGE_PATCH_SET + " \\d+:[^\\n]*(?:\\s+\\(\\d+ " +
                        GERRIT_DEFAULT_MESSAGE_COMMENTS + "?\\)\\s*)?",
                Pattern.DOTALL
        );
        log.debug("ClientMessageCleaner initialized with bot mention pattern: {}", botMentionPattern);
    }

    public ClientMessageCleaner removeHeadings() {
        log.debug("Removing headings from message.");
        message = messageHeadingPattern.matcher(message).replaceAll("");
        log.debug("Message after removing headings: {}", message);
        return this;
    }

    public ClientMessageCleaner removeMentions() {
        log.debug("Removing bot mentions from message.");
        message = botMentionPattern.matcher(message).replaceAll("").trim();
        log.debug("Message after removing mentions: {}", message);
        return this;
    }

    public ClientMessageCleaner removeCommands() {
        log.debug("Removing commands from message.");
        message = clientCommandCleaner.removeCommands(message);
        log.debug("Message after removing commands: {}", message);
        return this;
    }

    public ClientMessageCleaner removeDebugCodeBlocks() {
        log.debug("Removing debug code blocks for review.");
        message = debugCodeBlocksCleaner.removeDebugCodeBlocks(message);
        log.debug("Message after removing debug code blocks: {}", message);
        return this;
    }
}
