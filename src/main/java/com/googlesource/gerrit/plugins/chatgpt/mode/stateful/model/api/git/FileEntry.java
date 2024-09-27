package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.git;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class FileEntry {
    private String path;
    private String content;
    private long size;
}