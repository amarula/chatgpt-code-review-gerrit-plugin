package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptCreateVectorStoreRequest {
    private String name;
    @SerializedName("file_ids")
    private String[] fileIds;
}
