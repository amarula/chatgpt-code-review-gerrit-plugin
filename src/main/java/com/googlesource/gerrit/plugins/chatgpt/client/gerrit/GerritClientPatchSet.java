package com.googlesource.gerrit.plugins.chatgpt.client.gerrit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.chatgpt.client.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.client.ClientCommands;
import com.googlesource.gerrit.plugins.chatgpt.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.client.model.InputFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.client.model.OutputFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.utils.SingletonManager;
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

    public GerritClientPatchSet(Configuration config) {
        super(config);
        diffs = new ArrayList<>();
    }

    public String getPatchSet(GerritChange change) throws Exception {
        int revisionBase = retrieveRevisionBase(change);
        log.debug("Revision base: {}", revisionBase);

        List<String> files = getAffectedFiles(change.getFullChangeId(), revisionBase);
        log.debug("Patch files: {}", files);

        String fileDiffsJson = getFileDiffsJson(change.getFullChangeId(), files, revisionBase);
        log.debug("File diffs: {}", fileDiffsJson);

        return fileDiffsJson;
    }

    private boolean isChangeSetBased(GerritChange change) {
        ClientCommands clientCommands = SingletonManager.getInstance(ClientCommands.class, change);
        return change.getIsCommentEvent() || clientCommands.getForcedReviewChangeSet();
    }

    private int retrieveRevisionBase(GerritChange change) throws Exception {
        if (isChangeSetBased(change)) {
            return 0;
        }
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritPatchSetRevisionsUri(change.getFullChangeId()));
        log.debug("Retrieve Revision URI: '{}'", uri);
        JsonObject reviews = forwardGetRequestReturnJsonObject(uri);
        try {
            Set<String> revisions = reviews.get("revisions").getAsJsonObject().keySet();
            return revisions.size() -1;
        }
        catch (Exception e) {
            log.error("Could not retrieve revisions for PatchSet with fullChangeId: {}", change.getFullChangeId(), e);
            throw e;
        }
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
        InputFileDiff inputFileDiff = gson.fromJson(fileDiffJson, InputFileDiff.class);
        // Initialize the reduced output file diff with fields `meta_a` and `meta_b`
        OutputFileDiff outputFileDiff = new OutputFileDiff(inputFileDiff.getMetaA(), inputFileDiff.getMetaB());
        FileDiffProcessed fileDiffProcessed = new FileDiffProcessed(config, isCommitMessage, inputFileDiff);
        fileDiffsProcessed.put(filename, fileDiffProcessed);
        outputFileDiff.setContent(fileDiffProcessed.getOutputDiffContent());
        diffs.add(gson.toJson(outputFileDiff));
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
