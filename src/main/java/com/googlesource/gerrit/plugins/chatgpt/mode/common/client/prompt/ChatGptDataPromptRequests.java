package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptMessageItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.CHAT_GPT_ROLE_USER;

@Slf4j
public class ChatGptDataPromptRequests extends ChatGptDataPromptBase {
    protected ChatGptMessageItem messageItem;
    protected List<ChatGptRequestMessage> messageHistory;

    public ChatGptDataPromptRequests(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientData gerritClientData,
            Localizer localizer
    ) {
        super(config, changeSetData, gerritClientData, localizer);
        commentProperties = commentData.getCommentProperties();
        log.debug("ChatGptDataPromptRequests initialized with comment properties.");
    }

    public void addMessageItem(int i) {
        log.debug("Adding message item for comment index: {}", i);
        ChatGptMessageItem messageItem = getMessageItem(i);
        messageItem.setId(i);
        messageItems.add(messageItem);
        log.debug("Added message item: {}", messageItem);
    }

    @Override
    protected ChatGptMessageItem getMessageItem(int i) {
        messageItem = super.getMessageItem(i);
        log.debug("Retrieving extended message item for index: {}", i);
        messageHistory = gptMessageHistory.retrieveHistory(commentProperties.get(i));
        ChatGptRequestMessage request = extractLastUserMessageFromHistory();
        if (request != null) {
            messageItem.setRequest(request.getContent());
        }
        else {
            setRequestFromCommentProperty(messageItem, i);
        }
        log.debug("Message item after setting request content: {}", messageItem);
        return messageItem;
    }

    protected void setRequestFromCommentProperty(ChatGptMessageItem messageItem, int i) {
        GerritComment gerritComment = commentProperties.get(i);
        String cleanedMessage = gptMessageHistory.getCleanedMessage(gerritComment);
        log.debug("Getting cleaned Message: {}", cleanedMessage);
        messageItem.setRequest(cleanedMessage);
    }

    private ChatGptRequestMessage extractLastUserMessageFromHistory() {
        log.debug("Extracting last user message from history.");
        for (int i = messageHistory.size() - 1; i >= 0; i--) {
            if (CHAT_GPT_ROLE_USER.equals(messageHistory.get(i).getRole())) {
                ChatGptRequestMessage request = messageHistory.remove(i);
                log.debug("Last user message extracted: {}", request);
                return request;
            }
        }
        log.warn("Error extracting request from message history: no user message found in {}", messageHistory);
        return null;
    }
}
