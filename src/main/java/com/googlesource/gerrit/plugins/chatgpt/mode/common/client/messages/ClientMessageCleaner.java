package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands.ClientCommandCleaner;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksReview;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ClientMessageCleaner extends ClientMessageBase {
    private static final Pattern MESSAGE_HEADING_PATTERN = Pattern.compile(
            "^(?:Patch Set \\d+:[^\\n]*\\s+(?:\\(\\d+ comments?\\)\\s*)?)+");

    private final DebugCodeBlocksReview debugCodeBlocksReview;
    private final DebugCodeBlocksDynamicConfiguration debugCodeBlocksDynamicConfiguration;
    private final ClientCommandCleaner clientCommandCleaner;

    @Getter
    protected String message;

    public ClientMessageCleaner(Configuration config, String message, Localizer localizer) {
        super(config);
        this.message = message;
        debugCodeBlocksReview = new DebugCodeBlocksReview(localizer);
        debugCodeBlocksDynamicConfiguration = new DebugCodeBlocksDynamicConfiguration(localizer);
        clientCommandCleaner = new ClientCommandCleaner(config);
        log.debug("ClientMessageCleaner initialized with bot mention pattern: {}", botMentionPattern);
    }

    public ClientMessageCleaner removeHeadings() {
        log.debug("Removing headings from message.");
        message = MESSAGE_HEADING_PATTERN.matcher(message).replaceAll("");
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

    public ClientMessageCleaner removeDebugCodeBlocksReview() {
        log.debug("Removing debug code blocks for review.");
        message = debugCodeBlocksReview.removeDebugCodeBlocks(message);
        log.debug("Message after removing debug code blocks: {}", message);
        return this;
    }

    public ClientMessageCleaner removeDebugCodeBlocksDynamicConfiguration() {
        log.debug("Removing debug code blocks for dynamic configuration.");
        message = debugCodeBlocksDynamicConfiguration.removeDebugCodeBlocks(message);
        log.debug("Message after removing dynamic configuration debug code blocks: {}", message);
        return this;
    }
}
