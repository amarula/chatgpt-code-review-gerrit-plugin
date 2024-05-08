package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptCreateRunRequest {
    @SerializedName("assistant_id")
    private String assistantId;
}
