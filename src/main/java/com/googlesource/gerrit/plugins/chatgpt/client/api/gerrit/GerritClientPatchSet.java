package com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritPatchSetFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritReviewFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.settings.DynamicSettings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.*;

@Slf4j
public class GerritClientPatchSet extends GerritClientAccount {
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    private final List<String> diffs;
    private boolean isCommitMessage;
    @Getter
    private Integer revisionBase = 0;

    public GerritClientPatchSet(Configuration config) {
        super(config);
        diffs = new ArrayList<>();
    }

    public void retrieveRevisionBase(GerritChange change) {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritPatchSetRevisionsUri(change.getFullChangeId()));
        log.debug("Retrieve Revision URI: '{}'", uri);
        try {
            JsonObject reviews = forwardGetRequestReturnJsonObject(uri);
            Set<String> revisions = reviews.get("revisions").getAsJsonObject().keySet();
            revisionBase = revisions.size() -1;
        }
        catch (Exception e) {
            log.error("Could not retrieve revisions for PatchSet with fullChangeId: {}", change.getFullChangeId(), e);
            revisionBase = 0;
        }
    }

    public String getPatchSet(GerritChange change) throws Exception {
        int revisionBase = getChangeSetRevisionBase(change);
        log.debug("Revision base: {}", revisionBase);

        List<String> files = getAffectedFiles(change.getFullChangeId(), revisionBase);
        log.debug("Patch files: {}", files);

        String fileDiffsJson = getFileDiffsJson(change.getFullChangeId(), files, revisionBase);
        log.debug("File diffs: {}", fileDiffsJson);

        return fileDiffsJson;
    }

    private boolean isChangeSetBased(GerritChange change) {
        return !DynamicSettings.getInstance(change).getForcedReviewLastPatchSet();
    }

    private int getChangeSetRevisionBase(GerritChange change) {
        return isChangeSetBased(change) ? 0 : revisionBase;
    }

    private List<String> getAffectedFiles(String fullChangeId, int revisionBase) throws Exception {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritPatchSetFilesUri(fullChangeId)
                + UriResourceLocator.gerritRevisionBasePostfixUri(revisionBase));
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
        GerritPatchSetFileDiff gerritPatchSetFileDiff = gson.fromJson(fileDiffJson, GerritPatchSetFileDiff.class);
        // Initialize the reduced file diff for the Gerrit review with fields `meta_a` and `meta_b`
        GerritReviewFileDiff gerritReviewFileDiff = new GerritReviewFileDiff(gerritPatchSetFileDiff.getMetaA(),
                gerritPatchSetFileDiff.getMetaB());
        FileDiffProcessed fileDiffProcessed = new FileDiffProcessed(config, isCommitMessage, gerritPatchSetFileDiff);
        fileDiffsProcessed.put(filename, fileDiffProcessed);
        gerritReviewFileDiff.setContent(fileDiffProcessed.getReviewDiffContent());
        diffs.add(gson.toJson(gerritReviewFileDiff));
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
                    + UriResourceLocator.gerritPatchSetFilesUri(fullChangeId)
                    + UriResourceLocator.gerritDiffPostfixUri(filename)
                    + UriResourceLocator.gerritRevisionBasePostfixUri(revisionBase));
            log.debug("getFileDiffsJson URI: '{}'", uri);
            String fileDiffJson = forwardGetRequest(uri).replaceAll("^[')\\]}]+", "");
            processFileDiff(filename, fileDiffJson);
        }
        diffs.add(String.format("{\"changeId\": \"%s\"}", fullChangeId));
        return "[" + String.join(",", diffs) + "]\n";
    }

}
