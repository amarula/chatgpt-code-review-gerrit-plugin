package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatGptToolResources {
    @SerializedName("file_search")
    private VectorStoreIds fileSearch;

    @Data
    @AllArgsConstructor
    public static class VectorStoreIds {
        @SerializedName("vector_store_ids")
        private String[] vectorStoreIds;
    }
}