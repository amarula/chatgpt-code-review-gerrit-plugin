package com.googlesource.gerrit.plugins.chatgpt.model.data;

import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritComment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
@Data
public class CommentData {
    private List<GerritComment> commentProperties;
    private HashMap<String, GerritComment> commentMap;
    private HashMap<String, GerritComment> patchSetCommentMap;
}
