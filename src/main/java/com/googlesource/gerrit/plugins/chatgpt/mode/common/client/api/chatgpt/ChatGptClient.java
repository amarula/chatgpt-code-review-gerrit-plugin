package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
abstract public class ChatGptClient {
    protected boolean isCommentEvent = false;
    @Getter
    protected String requestBody;

    protected String extractContent(Configuration config, String body) throws Exception {
        if (config.getGptStreamOutput() && !isCommentEvent) {
            StringBuilder finalContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    extractContentFromLine(line).ifPresent(finalContent::append);
                }
            }
            return finalContent.toString();
        }
        else {
            ChatGptResponseUnstreamed chatGptResponseUnstreamed =
                    getGson().fromJson(body, ChatGptResponseUnstreamed.class);
            return getResponseContent(chatGptResponseUnstreamed.getChoices().get(0).getMessage().getToolCalls());
        }
    }

    protected boolean validateResponse(String contentExtracted, String changeId, int attemptInd) {
        ChatGptResponseContent chatGptResponseContent = convertResponseContentFromJson(contentExtracted);
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

    protected String getResponseContent(List<ChatGptToolCall> toolCalls) {
        if (toolCalls.size() > 1) {
            return mergeToolCalls(toolCalls);
        } else {
            return getArgumentAtInd(toolCalls, 0);
        }
    }

    protected Optional<String> extractContentFromLine(String line) {
        String dataPrefix = "data: {\"id\"";

        if (!line.startsWith(dataPrefix)) {
            return Optional.empty();
        }
        ChatGptResponseStreamed chatGptResponseStreamed =
                getGson().fromJson(line.substring("data: ".length()), ChatGptResponseStreamed.class);
        ChatGptResponseMessage delta = chatGptResponseStreamed.getChoices().get(0).getDelta();
        if (delta == null || delta.getToolCalls() == null) {
            return Optional.empty();
        }
        String content = getResponseContent(delta.getToolCalls());
        return Optional.ofNullable(content);
    }

    private ChatGptResponseContent convertResponseContentFromJson(String content) {
        return getGson().fromJson(content, ChatGptResponseContent.class);
    }

    private String getArgumentAtInd(List<ChatGptToolCall> toolCalls, int ind) {
        return toolCalls.get(ind).getFunction().getArguments();
    }

    private String mergeToolCalls(List<ChatGptToolCall> toolCalls) {
        ChatGptResponseContent responseContent = convertResponseContentFromJson(getArgumentAtInd(toolCalls, 0));
        for (int ind = 1; ind < toolCalls.size(); ind++) {
            responseContent.getReplies().addAll(
                    convertResponseContentFromJson(getArgumentAtInd(toolCalls, ind)).getReplies()
            );
        }
        return getGson().toJson(responseContent);
    }

}
