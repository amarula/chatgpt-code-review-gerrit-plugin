package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data;

import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class GerritClientData {
    private IGerritClientPatchSet gerritClientPatchSet;
    private List<GerritComment> detailComments;
    private CommentData commentData;
    private Integer revisionBase;

    public List<GerritComment> getCommentProperties() {
        return commentData.getCommentProperties();
    }

    public int getOneBasedRevisionBase() {
        return revisionBase +1;
    }
}
