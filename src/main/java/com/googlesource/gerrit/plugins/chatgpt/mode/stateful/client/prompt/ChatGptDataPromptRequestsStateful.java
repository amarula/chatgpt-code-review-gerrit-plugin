package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.prompt.IChatGptDataPrompt;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptDataPromptRequests;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class ChatGptDataPromptRequestsStateful extends ChatGptDataPromptRequests implements IChatGptDataPrompt {
    private static final String REPLY_MESSAGE_REFERENCE = " (Ref. message \"%s\")";

    public ChatGptDataPromptRequestsStateful(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
        log.debug("ChatGptDataPromptRequestsStateful initialized");
    }

    @Override
    protected ChatGptMessageItem getMessageItem(int i) {
        log.debug("Getting stateful Message Item");
        ChatGptMessageItem messageItem = new ChatGptMessageItem();
        setRequestFromCommentProperty(messageItem, i);
        String inReplyToMessage = getReferenceToLastMessage(i);
        if (inReplyToMessage != null) {
            log.debug("In-reply-to message found: {}", inReplyToMessage);
            messageItem.appendToRequest(inReplyToMessage);
        }
        log.debug("Returned Message Item: {}", messageItem);
        return messageItem;
    }

    private String getReferenceToLastMessage(int i) {
        GerritComment comment = commentProperties.get(i);
        log.debug("Getting reference to last message for comment {}", comment);
        String inReplyToId = comment.getInReplyTo();
        if (inReplyToId == null || inReplyToId.isEmpty()) {
            return null;
        }
        HashMap<String, GerritComment> commentMap = gptMessageHistory.getCommentMap();
        log.debug("Getting Comment Map: {}", commentMap);
        if (commentMap.isEmpty()) {
            return null;
        }
        GerritComment inReplyToComment = commentMap.get(inReplyToId);
        if (inReplyToComment == null) {
            return null;
        }
        String referenceToLastMessage = String.format(REPLY_MESSAGE_REFERENCE, gptMessageHistory.getCleanedMessage(inReplyToComment));
        log.debug("Reference to last message: {}", referenceToLastMessage);
        return referenceToLastMessage;
    }
}
