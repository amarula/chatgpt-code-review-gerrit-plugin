package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptCreateAssistantRequestBody {
    private String name;
    private String description;
    private String instructions;
    private String model;
    private Double temperature;
    private ChatGptTool[] tools;
    @SerializedName("tool_resources")
    private ChatGptToolResources toolResources;
}
