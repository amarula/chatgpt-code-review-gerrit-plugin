package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequestMessage;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolChoice;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatGptCompletionRequest {
    private String model;
    private boolean stream;
    private double temperature;
    private int seed;
    private List<ChatGptRequestMessage> messages;
    private ChatGptTool[] tools;
    @SerializedName("tool_choice")
    private ChatGptToolChoice toolChoice;
}
