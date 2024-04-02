package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.patch.code;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.patch.diff.DiffContent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.TreeMap;

@AllArgsConstructor
@Data
public class CodeFinderDiff {
    private DiffContent content;
    private TreeMap<Integer, Integer> charToLineMap;
}
