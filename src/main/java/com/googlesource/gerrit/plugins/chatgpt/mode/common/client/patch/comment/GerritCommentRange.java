package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.comment;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.code.InlineCode;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class GerritCommentRange {
    private final HashMap<String, FileDiffProcessed> fileDiffsProcessed;

    public GerritCommentRange(GerritClient gerritClient, GerritChange change) {
        fileDiffsProcessed = gerritClient.getFileDiffsProcessed(change);
        log.debug("Initialized GerritCommentRange with file diffs processed for change: {}", change.getFullChangeId());
    }

    public Optional<GerritCodeRange> getGerritCommentRange(ChatGptReplyItem replyItem) {
        log.debug("Retrieving Gerrit comment range for reply item: {}", replyItem);
        Optional<GerritCodeRange> gerritCommentRange = Optional.empty();
        String filename = replyItem.getFilename();
        if (filename == null || filename.equals("/COMMIT_MSG")) {
            log.debug("Filename is null or COMMIT_MSG, skipping code range extraction.");
            return gerritCommentRange;
        }
        if (replyItem.getCodeSnippet() == null) {
            log.info("CodeSnippet is null in reply '{}'.", replyItem);
            return gerritCommentRange;
        }
        if (!fileDiffsProcessed.containsKey(filename)) {
            log.info("Filename '{}' not found for reply '{}'.\nFileDiffsProcessed = {}", filename, replyItem,
                    fileDiffsProcessed);
            return gerritCommentRange;
        }
        InlineCode inlineCode = new InlineCode(fileDiffsProcessed.get(filename));
        gerritCommentRange = inlineCode.findCommentRange(replyItem);
        if (gerritCommentRange.isEmpty()) {
            log.info("Inline code not found for reply {}", replyItem);
        }
        else {
            log.debug("Found inline code range: {}", gerritCommentRange.get());
        }
        return gerritCommentRange;
    }
}
