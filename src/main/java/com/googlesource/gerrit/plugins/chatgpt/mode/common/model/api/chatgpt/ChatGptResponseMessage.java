package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ChatGptResponseMessage {
    private String role;
    private String type;
    @SerializedName("tool_calls")
    private List<ChatGptToolCall> toolCalls;
    @SerializedName("message_creation")
    private MessageCreation messageCreation;

    @Data
    public static class MessageCreation {
        @SerializedName("message_id")
        private String messageId;
    }
}
