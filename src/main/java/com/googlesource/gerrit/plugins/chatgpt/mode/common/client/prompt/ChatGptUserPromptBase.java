package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.code.InlineCode;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.CommentData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public abstract class ChatGptUserPromptBase {
    protected final GerritClientData gerritClientData;
    protected final HashMap<String, FileDiffProcessed> fileDiffsProcessed;
    protected final CommentData commentData;
    @Getter
    protected final List<ChatGptMessageItem> messageItems;

    protected ChatGptHistory gptMessageHistory;
    @Getter
    protected List<GerritComment> commentProperties;

    public ChatGptUserPromptBase(Configuration config, ChangeSetData changeSetData, GerritChange change, GerritClientData gerritClientData) {
        this.gerritClientData = gerritClientData;
        fileDiffsProcessed = gerritClientData.getFileDiffsProcessed();
        commentData = gerritClientData.getCommentData();
        gptMessageHistory = new ChatGptHistory(config, changeSetData, change, gerritClientData);
        messageItems = new ArrayList<>();
    }

    abstract void addMessageItem(int i);

    protected ChatGptMessageItem getMessageItem(int i) {
        ChatGptMessageItem messageItem = new ChatGptMessageItem();
        GerritComment commentProperty = commentProperties.get(i);
        if (commentProperty.getLine() != null || commentProperty.getRange() != null) {
            String filename = commentProperty.getFilename();
            FileDiffProcessed fileDiffProcessed = fileDiffsProcessed.get(filename);
            if (fileDiffProcessed == null) {
                return messageItem;
            }
            InlineCode inlineCode = new InlineCode(fileDiffProcessed);
            messageItem.setFilename(filename);
            messageItem.setLineNumber(commentProperty.getLine());
            messageItem.setCodeSnippet(inlineCode.getInlineCode(commentProperty));
        }

        return messageItem;
    }

    protected void setHistories(ChatGptMessageItem messageItem, List<ChatGptRequestMessage> messageHistories) {
        if (!messageHistories.isEmpty()) {
            messageItem.setHistory(messageHistories);
        }
    }

}
