package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class GerritPatchSetDetail {
    private Labels labels;
    private List<GerritComment> messages;
    @SerializedName("work_in_progress")
    private Boolean workInProgress;

    @Data
    public static class Labels {
        @SerializedName("Code-Review")
        private CodeReview codeReview;
    }

    @Data
    public static class CodeReview {
        private List<Permission> all;
    }

    @Data
    public static class Permission {
        private Integer value;
        private String date;
        @SerializedName("permitted_voting_range")
        private GerritPermittedVotingRange permittedVotingRange;
        @SerializedName("_account_id")
        private int accountId;
    }

}
