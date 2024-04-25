package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.gerrit;

import com.google.inject.Inject;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPatchSetFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritReviewFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getNoEscapedGson;

@Slf4j
public class GerritClientPatchSetStateless extends GerritClientPatchSet implements IGerritClientPatchSet {
    private final List<String> diffs;
    private boolean isCommitMessage;

    @Inject
    public GerritClientPatchSetStateless(Configuration config) {
        super(config);
        diffs = new ArrayList<>();
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
        int revisionBase = getChangeSetRevisionBase(changeSetData);
        log.debug("Revision base: {}", revisionBase);

        List<String> files = getAffectedFiles(change, revisionBase);
        log.debug("Patch files: {}", files);

        String fileDiffsJson = getFileDiffsJson(change, files, revisionBase);
        log.debug("File diffs: {}", fileDiffsJson);

        return fileDiffsJson;
    }

    private List<String> getAffectedFiles(GerritChange change, int revisionBase) throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            Map<String, FileInfo> files =
                config
                    .getGerritApi()
                    .changes()
                    .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                    .current()
                    .files(revisionBase);
            return files.entrySet().stream()
                .filter(
                    fileEntry -> {
                      String filename = fileEntry.getKey();
                      if (!filename.equals("/COMMIT_MSG") || config.getGptReviewCommitMessages()) {
                        if (fileEntry.getValue().size > config.getMaxReviewFileSize()) {
                          log.info(
                              "File '{}' not reviewed because its size exceeds the fixed maximum allowable size.",
                              filename);
                        } else {
                          return true;
                        }
                      }
                      return false;
                    })
                .map(Map.Entry::getKey)
                .collect(toList());
        }
    }

    private void processFileDiff(String filename, DiffInfo diff) {
        log.debug("FileDiff content processed: {}", filename);

        GerritPatchSetFileDiff gerritPatchSetFileDiff = new GerritPatchSetFileDiff();
        Optional.ofNullable(diff.metaA)
            .ifPresent(
                meta -> gerritPatchSetFileDiff.setMetaA(GerritClientPatchSetStateless.toMeta(meta)));
        Optional.ofNullable(diff.metaB)
            .ifPresent(
                meta -> gerritPatchSetFileDiff.setMetaB(GerritClientPatchSetStateless.toMeta(meta)));
        Optional.ofNullable(diff.content)
            .ifPresent(
                content ->
                    gerritPatchSetFileDiff.setContent(
                        content.stream()
                            .map(GerritClientPatchSetStateless::toContent)
                            .collect(toList())));

        // Initialize the reduced file diff for the Gerrit review with fields `meta_a` and `meta_b`
        GerritReviewFileDiff gerritReviewFileDiff = new GerritReviewFileDiff(gerritPatchSetFileDiff.getMetaA(),
                gerritPatchSetFileDiff.getMetaB());
        FileDiffProcessed fileDiffProcessed = new FileDiffProcessed(config, isCommitMessage, gerritPatchSetFileDiff);
        fileDiffsProcessed.put(filename, fileDiffProcessed);
        gerritReviewFileDiff.setContent(fileDiffProcessed.getReviewDiffContent());
        diffs.add(getNoEscapedGson().toJson(gerritReviewFileDiff));
    }

    private String getFileDiffsJson(GerritChange change, List<String> files, int revisionBase) throws Exception {
        List<String> enabledFileExtensions = config.getEnabledFileExtensions();
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            for (String filename : files) {
                isCommitMessage = filename.equals("/COMMIT_MSG");
                if (!isCommitMessage && (filename.lastIndexOf(".") < 1 ||
                        !enabledFileExtensions.contains(filename.substring(filename.lastIndexOf("."))))) {
                    continue;
                }
                DiffInfo diff =
                    config
                        .getGerritApi()
                        .changes()
                        .id(
                            change.getProjectName(),
                            change.getBranchNameKey().shortName(),
                            change.getChangeKey().get())
                        .current()
                        .file(filename)
                        .diff(revisionBase);
                processFileDiff(filename, diff);
            }
        }
        diffs.add(String.format("{\"changeId\": \"%s\"}", change.getFullChangeId()));
        return "[" + String.join(",", diffs) + "]\n";
    }

    private static GerritFileDiff.Meta toMeta(DiffInfo.FileMeta input) {
        GerritFileDiff.Meta meta = new GerritFileDiff.Meta();
        meta.setContentType(input.contentType);
        meta.setName(input.name);
        return meta;
    }

    private static GerritPatchSetFileDiff.Content toContent(DiffInfo.ContentEntry input) {
        GerritPatchSetFileDiff.Content content = new GerritPatchSetFileDiff.Content();
        content.a = input.a;
        content.b = input.b;
        content.ab = input.ab;
        return content;
    }
}
