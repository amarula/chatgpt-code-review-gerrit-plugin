package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.jsonToClass;

@Slf4j
abstract public class ChatGptClient extends ChatGptClientBase {
    protected boolean isCommentEvent = false;
    @Getter
    protected String requestBody;

    public ChatGptClient(Configuration config) {
        super(config);
        log.debug("ChatGptClient initialized with configuration.");
    }

    protected ChatGptResponseContent extractContent(Configuration config, String body) throws Exception {
        log.debug("Extracting content with streaming enabled: {}", config.getGptStreamOutput());
        if (config.getGptStreamOutput() && !isCommentEvent) {
            StringBuilder finalContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    extractContentFromLine(line).ifPresent(finalContent::append);
                }
            }
            return convertResponseContentFromJson(finalContent.toString());
        }
        else {
            ChatGptResponseUnstreamed chatGptResponseUnstreamed = jsonToClass(body, ChatGptResponseUnstreamed.class);
            return getResponseContent(chatGptResponseUnstreamed.getChoices().get(0).getMessage().getToolCalls());
        }
    }

    protected boolean validateResponse(ChatGptResponseContent chatGptResponseContent, String changeId, int attemptInd) {
        log.debug("Validating response for change ID: {}, attempt: {}", changeId, attemptInd);
        String returnedChangeId = chatGptResponseContent.getChangeId();
        // A response is considered valid if either no changeId is returned or the changeId returned matches the one
        // provided in the request
        boolean isValidated = returnedChangeId == null || changeId.equals(returnedChangeId);
        if (!isValidated) {
            log.error("ChangedId mismatch error (attempt #{}).\nExpected value: {}\nReturned value: {}", attemptInd,
                    changeId, returnedChangeId);
        }
        return isValidated;
    }

    protected ChatGptResponseContent getResponseContent(List<ChatGptToolCall> toolCalls) {
        log.debug("Getting response content from tool calls: {}", toolCalls);
        if (toolCalls.size() > 1) {
            return mergeToolCalls(toolCalls);
        } else {
            return getArgumentAsResponse(toolCalls, 0);
        }
    }

    protected Optional<String> extractContentFromLine(String line) {
        log.debug("Extracting content from line \"{}\".", line);
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatGptResponseStreamed chatGptResponseStreamed =
                jsonToClass(line.substring("data: ".length()), ChatGptResponseStreamed.class);
        ChatGptResponseMessage delta = chatGptResponseStreamed.getChoices().get(0).getDelta();
        if (delta == null || delta.getToolCalls() == null) {
            return Optional.empty();
        }
        String content = getArgumentAsString(delta.getToolCalls(), 0);
        return Optional.ofNullable(content);
    }

    private ChatGptResponseContent mergeToolCalls(List<ChatGptToolCall> toolCalls) {
        log.debug("Merging responses from multiple tool calls.");
        ChatGptResponseContent responseContent = getArgumentAsResponse(toolCalls, 0);
        for (int ind = 1; ind < toolCalls.size(); ind++) {
            responseContent.getReplies().addAll(
                    getArgumentAsResponse(toolCalls, ind).getReplies()
            );
        }
        return responseContent;
    }
}
