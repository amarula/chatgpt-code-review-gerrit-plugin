package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.filename;

import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FilenameSanitizer {
    private final List<String> patchSetFiles;

    public FilenameSanitizer(GerritClient gerritClient, GerritChange change) {
        IGerritClientPatchSet gerritClientPatchSet = gerritClient.getClientData(change).getGerritClientPatchSet();
        patchSetFiles = gerritClientPatchSet.getPatchSetFiles();
        log.debug("Initialized Patch set files: {}", patchSetFiles);
    }

    public void sanitizeFilename(ChatGptReplyItem replyItem) {
        String filename = replyItem.getFilename();
        log.debug("Sanitizing filename: {}", filename);
        if (filename == null || filename.isEmpty() || patchSetFiles.contains(filename)) {
            return;
        }
        String sanitizedFilename = patchSetFiles
                .stream()
                .filter(s -> s.contains(filename))
                .findFirst()
                .orElse(null);
        if (sanitizedFilename == null) {
            log.warn("Filename '{}' not sanitized. PatchSet Files: {}", filename, patchSetFiles);
            return;
        }
        log.debug("Filename sanitized: {}", sanitizedFilename);

        replyItem.setFilename(sanitizedFilename);
    }
}
