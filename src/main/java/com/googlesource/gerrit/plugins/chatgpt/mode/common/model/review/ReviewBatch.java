package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import lombok.Data;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;

@Data
public class ReviewBatch {
    private String id;
    private String content;
    private String filename;
    private Integer line;
    private GerritCodeRange range;

    public String getFilename() {
        return filename == null ? GERRIT_PATCH_SET_FILENAME : filename;
    }
}
