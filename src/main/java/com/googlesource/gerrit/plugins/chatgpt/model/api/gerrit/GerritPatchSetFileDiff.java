package com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GerritPatchSetFileDiff extends GerritFileDiff {
    private List<Content> content;

    @Data
    public static class Content {
        public List<String> a;
        public List<String> b;
        public List<String> ab;
    }

}
