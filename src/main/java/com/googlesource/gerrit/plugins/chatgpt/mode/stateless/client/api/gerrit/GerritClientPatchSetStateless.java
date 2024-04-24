package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.gerrit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPatchSetFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritReviewFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.UriResourceLocatorStateless;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
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

        List<String> files = getAffectedFiles(change.getFullChangeId(), revisionBase);
        log.debug("Patch files: {}", files);

        String fileDiffsJson = getFileDiffsJson(change.getFullChangeId(), files, revisionBase);
        log.debug("File diffs: {}", fileDiffsJson);

        return fileDiffsJson;
    }

    private List<String> getAffectedFiles(String fullChangeId, int revisionBase) throws Exception {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocatorStateless.gerritPatchSetFilesUri(fullChangeId)
                + UriResourceLocatorStateless.gerritRevisionBasePostfixUri(revisionBase));
        log.debug("Affected Files URI: '{}'", uri);
        JsonObject affectedFileMap = forwardGetRequestReturnJsonObject(uri);
        List<String> files = new ArrayList<>();
        for (Map.Entry<String, JsonElement> file : affectedFileMap.entrySet()) {
            String filename = file.getKey();
            if (!filename.equals("/COMMIT_MSG") || config.getGptReviewCommitMessages()) {
                int size = Integer.parseInt(file.getValue().getAsJsonObject().get("size").getAsString());
                if (size > config.getMaxReviewFileSize()) {
                    log.info("File '{}' not reviewed because its size exceeds the fixed maximum allowable size.",
                            filename);
                }
                else {
                    files.add(filename);
                }
            }
        }
        return files;
    }

    private void processFileDiff(String filename, String fileDiffJson) {
        log.debug("FileDiff Json processed: {}", fileDiffJson);
        GerritPatchSetFileDiff gerritPatchSetFileDiff = getGson().fromJson(fileDiffJson, GerritPatchSetFileDiff.class);
        // Initialize the reduced file diff for the Gerrit review with fields `meta_a` and `meta_b`
        GerritReviewFileDiff gerritReviewFileDiff = new GerritReviewFileDiff(gerritPatchSetFileDiff.getMetaA(),
                gerritPatchSetFileDiff.getMetaB());
        FileDiffProcessed fileDiffProcessed = new FileDiffProcessed(config, isCommitMessage, gerritPatchSetFileDiff);
        fileDiffsProcessed.put(filename, fileDiffProcessed);
        gerritReviewFileDiff.setContent(fileDiffProcessed.getReviewDiffContent());
        diffs.add(getNoEscapedGson().toJson(gerritReviewFileDiff));
    }

    private String getFileDiffsJson(String fullChangeId, List<String> files, int revisionBase) throws Exception {
        List<String> enabledFileExtensions = config.getEnabledFileExtensions();
        for (String filename : files) {
            isCommitMessage = filename.equals("/COMMIT_MSG");
            if (!isCommitMessage && (filename.lastIndexOf(".") < 1 ||
                    !enabledFileExtensions.contains(filename.substring(filename.lastIndexOf("."))))) {
                continue;
            }
            URI uri = URI.create(config.getGerritAuthBaseUrl()
                    + UriResourceLocatorStateless.gerritPatchSetFilesUri(fullChangeId)
                    + UriResourceLocatorStateless.gerritDiffPostfixUri(filename)
                    + UriResourceLocatorStateless.gerritRevisionBasePostfixUri(revisionBase));
            log.debug("getFileDiffsJson URI: '{}'", uri);
            String fileDiffJson = forwardGetRequest(uri).replaceAll("^[')\\]}]+", "");
            processFileDiff(filename, fileDiffJson);
        }
        diffs.add(String.format("{\"changeId\": \"%s\"}", fullChangeId));
        return "[" + String.join(",", diffs) + "]\n";
    }

}
