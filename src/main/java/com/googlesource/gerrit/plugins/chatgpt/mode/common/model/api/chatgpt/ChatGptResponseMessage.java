package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ChatGptResponseMessage {
    private String role;
    @SerializedName("tool_calls")
    private List<ChatGptToolCall> toolCalls;
}
