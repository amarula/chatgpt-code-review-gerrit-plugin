package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPatchSetFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritReviewFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.matchesExtensionList;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getNoEscapedGson;
import static java.util.stream.Collectors.toList;

@Slf4j
public class GerritClientPatchSet extends GerritClientAccount {
    protected final List<String> diffs;

    @Getter
    protected Integer revisionBase = 0;

    private boolean isCommitMessage;

    public GerritClientPatchSet(Configuration config, AccountCache accountCache) {
        super(config, accountCache);
        diffs = new ArrayList<>();
    }

    public void retrieveRevisionBase(GerritChange change) {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            ChangeInfo changeInfo =
                config
                    .getGerritApi()
                    .changes()
                    .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                    .get(ListChangesOption.ALL_REVISIONS);
            revisionBase =
                Optional.ofNullable(changeInfo)
                    .map(info -> info.revisions)
                    .map(revisions -> revisions.size() - 1)
                    .orElse(0);
        }
        catch (Exception e) {
            log.error("Could not retrieve revisions for PatchSet with fullChangeId: {}", change.getFullChangeId(), e);
            revisionBase = 0;
        }
    }

    protected int getChangeSetRevisionBase(ChangeSetData changeSetData) {
        return isChangeSetBased(changeSetData) ? 0 : revisionBase;
    }

    protected void retrieveFileDiff(GerritChange change, List<String> files, int revisionBase) throws Exception {
        List<String> enabledFileExtensions = config.getEnabledFileExtensions();
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            for (String filename : files) {
                isCommitMessage = filename.equals("/COMMIT_MSG");
                if (!isCommitMessage && !matchesExtensionList(filename, enabledFileExtensions)) {
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
    }

    private boolean isChangeSetBased(ChangeSetData changeSetData) {
        return !changeSetData.getForcedReviewLastPatchSet();
    }

    private void processFileDiff(String filename, DiffInfo diff) {
        log.debug("FileDiff content processed: {}", filename);

        GerritPatchSetFileDiff gerritPatchSetFileDiff = new GerritPatchSetFileDiff();
        Optional.ofNullable(diff.metaA)
                .ifPresent(
                        meta -> gerritPatchSetFileDiff.setMetaA(GerritClientPatchSet.toMeta(meta)));
        Optional.ofNullable(diff.metaB)
                .ifPresent(
                        meta -> gerritPatchSetFileDiff.setMetaB(GerritClientPatchSet.toMeta(meta)));
        Optional.ofNullable(diff.content)
                .ifPresent(
                        content ->
                                gerritPatchSetFileDiff.setContent(
                                        content.stream()
                                                .map(GerritClientPatchSet::toContent)
                                                .collect(toList())));

        // Initialize the reduced file diff for the Gerrit review with fields `meta_a` and `meta_b`
        GerritReviewFileDiff gerritReviewFileDiff = new GerritReviewFileDiff(gerritPatchSetFileDiff.getMetaA(),
                gerritPatchSetFileDiff.getMetaB());
        FileDiffProcessed fileDiffProcessed = new FileDiffProcessed(config, isCommitMessage, gerritPatchSetFileDiff);
        fileDiffsProcessed.put(filename, fileDiffProcessed);
        gerritReviewFileDiff.setContent(fileDiffProcessed.getReviewDiffContent());
        diffs.add(getNoEscapedGson().toJson(gerritReviewFileDiff));
    }

    protected static GerritFileDiff.Meta toMeta(DiffInfo.FileMeta input) {
        GerritFileDiff.Meta meta = new GerritFileDiff.Meta();
        meta.setContentType(input.contentType);
        meta.setName(input.name);
        return meta;
    }

    protected static GerritPatchSetFileDiff.Content toContent(DiffInfo.ContentEntry input) {
        GerritPatchSetFileDiff.Content content = new GerritPatchSetFileDiff.Content();
        content.a = input.a;
        content.b = input.b;
        content.ab = input.ab;
        return content;
    }
}
