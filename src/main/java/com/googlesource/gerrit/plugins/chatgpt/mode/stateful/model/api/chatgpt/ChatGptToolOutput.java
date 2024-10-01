package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptToolOutput {
    @SerializedName("tool_call_id")
    private String toolCallId;
    private String output;
}
