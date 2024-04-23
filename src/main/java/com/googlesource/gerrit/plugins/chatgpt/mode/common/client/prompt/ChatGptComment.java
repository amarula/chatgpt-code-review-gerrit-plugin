package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.ClientMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatGptComment extends ClientBase {
    protected ClientMessage commentMessage;

    private final Integer gptAccountId;
    private final GerritChange change;

    public ChatGptComment(Configuration config, GerritChange change) {
        super(config);
        this.change = change;
        gptAccountId = ChangeSetDataHandler.getInstance(change).getGptAccountId();
    }

    protected String getCleanedMessage(GerritComment commentProperty) {
        commentMessage = new ClientMessage(config, change, commentProperty.getMessage());
        if (isFromAssistant(commentProperty)) {
            commentMessage.removeDebugMessages();
        }
        else {
            commentMessage.removeMentions().parseRemoveCommands().removeHeadings();
        }
        return commentMessage.getMessage();
    }

    protected boolean isFromAssistant(GerritComment commentProperty) {
        return commentProperty.getAuthor().getAccountId() == gptAccountId;
    }

}