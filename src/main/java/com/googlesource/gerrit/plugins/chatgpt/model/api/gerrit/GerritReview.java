package com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GerritReview {
    private Map<String, List<GerritComment>> comments;
    private String message;
    private Labels labels;

    @AllArgsConstructor
    @Data
    public static class Labels {
        @SerializedName("Code-Review")
        private int codeReview;
    }

}
