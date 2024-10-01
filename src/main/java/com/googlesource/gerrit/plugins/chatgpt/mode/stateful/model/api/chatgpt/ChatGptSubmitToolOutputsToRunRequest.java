package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatGptSubmitToolOutputsToRunRequest {
    @SerializedName("tool_outputs")
    private List<ChatGptToolOutput> toolOutputs;
}
