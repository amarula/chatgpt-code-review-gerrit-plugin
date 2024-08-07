package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit;

import com.google.gson.annotations.SerializedName;
import com.googlesource.gerrit.plugins.chatgpt.settings.Settings;
import lombok.Data;

@Data
public class GerritComment {
    private Author author;
    @SerializedName("change_message_id")
    private String changeMessageId;
    private Boolean unresolved;
    @SerializedName("patch_set")
    private Integer patchSet;
    private String id;
    private String tag;
    private Integer line;
    private GerritCodeRange range;
    @SerializedName("in_reply_to")
    private String inReplyTo;
    private String updated;
    // Field `date` is used by messages from PatchSet details
    private String date;
    private String message;
    @SerializedName("commit_id")
    private String commitId;
    // Metadata field that is set to the commented filename
    private String filename;

    @Data
    public static class Author {
        @SerializedName("_account_id")
        private int accountId;
        private String name;
        @SerializedName("display_name")
        private String displayName;
        private String email;
        private String username;
    }

    public boolean isAutogenerated() {
        return tag != null && tag.startsWith(Settings.GERRIT_AUTOGENERATED_PREFIX);
    }

    public boolean isPatchSetComment() {
        return filename != null && filename.equals(Settings.GERRIT_PATCH_SET_FILENAME);
    }

    public boolean isResolved() {
        // unresolved is null for the messages from Patch Set details
        return unresolved != null && !unresolved;
    }

    public int getOneBasedPatchSet() {
        return patchSet == null ? 1 : Math.max(patchSet, 1);
    }
}
