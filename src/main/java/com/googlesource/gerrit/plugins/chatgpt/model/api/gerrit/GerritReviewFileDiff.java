package com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.model.patch.diff.DiffContent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GerritReviewFileDiff extends GerritFileDiff {
    private List<DiffContent> content;

    public GerritReviewFileDiff(Meta metaA, Meta metaB) {
        this.metaA = metaA;
        this.metaB = metaB;
    }
}
