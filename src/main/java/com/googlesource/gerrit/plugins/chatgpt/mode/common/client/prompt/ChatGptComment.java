package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.ClientMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptComment extends ClientBase {
    protected ClientMessage commentMessage;

    private final ChangeSetData changeSetData;
    private final Localizer localizer;

    public ChatGptComment(Configuration config, ChangeSetData changeSetData, Localizer localizer) {
        super(config);
        this.changeSetData = changeSetData;
        this.localizer = localizer;
        log.debug("ChatGptComment initialized");
    }

    public String getCleanedMessage(GerritComment commentProperty) {
        log.debug("Cleaning message for comment property: {}", commentProperty);
        commentMessage = new ClientMessage(config, changeSetData, commentProperty.getMessage(), localizer);
        if (isFromAssistant(commentProperty)) {
            log.debug("Comment from assistant detected. Removing debug code blocks.");
            commentMessage.removeDebugCodeBlocksReview().removeDebugCodeBlocksDynamicSettings();
        }
        else {
            log.debug("Comment not from assistant. Removing mentions and commands.");
            commentMessage.removeMentions().removeCommands();
        }
        String cleanedMessage = commentMessage.removeHeadings().getMessage();
        log.debug("Cleaned message: {}", cleanedMessage);
        return cleanedMessage;
    }

    protected boolean isFromAssistant(GerritComment commentProperty) {
        boolean fromAssistant = commentProperty.getAuthor().getAccountId() == changeSetData.getGptAccountId();
        log.debug("Checking if comment is from assistant: {}", fromAssistant);
        return fromAssistant;
    }
}
