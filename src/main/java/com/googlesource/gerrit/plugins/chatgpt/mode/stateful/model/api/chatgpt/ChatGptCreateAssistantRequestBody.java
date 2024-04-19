package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptCreateAssistantRequestBody {
    String name;
    String description;
    String instructions;
    String model;
    Double temperature;
    @SerializedName("file_ids")
    String[] fileIds;
    ChatGptTool[] tools;
}
