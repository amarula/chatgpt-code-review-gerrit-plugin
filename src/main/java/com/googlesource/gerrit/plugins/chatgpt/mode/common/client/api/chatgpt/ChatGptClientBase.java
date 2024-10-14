package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.jsonToClass;

public abstract class ChatGptClientBase extends ClientBase {

    public ChatGptClientBase(Configuration config) {
        super(config);
    }

    protected ChatGptResponseContent convertResponseContentFromJson(String content) {
        return jsonToClass(content, ChatGptResponseContent.class);
    }

    protected ChatGptToolCall.Function getFunction(List<ChatGptToolCall> toolCalls, int ind) {
        return toolCalls.get(ind).getFunction();
    }

    protected String getArgumentAsString(List<ChatGptToolCall> toolCalls, int ind) {
        return getFunction(toolCalls, ind).getArguments();
    }

    protected ChatGptResponseContent getArgumentAsResponse(List<ChatGptToolCall> toolCalls, int ind) {
        return convertResponseContentFromJson(getArgumentAsString(toolCalls, ind));
    }

    protected <T> T getArgumentAsType(List<ChatGptToolCall> toolCalls, int ind, Class<T> clazz) {
        return jsonToClass(getArgumentAsString(toolCalls, ind), clazz);
    }
}
