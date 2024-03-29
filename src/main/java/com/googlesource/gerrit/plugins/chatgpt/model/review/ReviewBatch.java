package com.googlesource.gerrit.plugins.chatgpt.model.review;

import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritCodeRange;
import lombok.Data;

import static com.googlesource.gerrit.plugins.chatgpt.settings.StaticSettings.GERRIT_PATCH_SET_FILENAME;

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
