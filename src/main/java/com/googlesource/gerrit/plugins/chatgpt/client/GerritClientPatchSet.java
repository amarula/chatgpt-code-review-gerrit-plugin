package com.googlesource.gerrit.plugins.chatgpt.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.chatgpt.client.model.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.client.model.InputFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.client.model.OutputFileDiff;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.*;
import java.lang.reflect.Field;

@Slf4j
public class GerritClientPatchSet extends GerritClientAccount {
    private static final String[] COMMIT_MESSAGE_FILTER_OUT_PREFIXES = {
        "Parent:",
        "Author:",
        "AuthorDate:",
        "Commit:",
        "CommitDate:",
        "Change-Id:"
    };

    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    private boolean isCommitMessage;
    private final List<String> diffs;
    private List<String> newFileContent;

    public GerritClientPatchSet(Configuration config) {
        super(config);
        diffs = new ArrayList<>();
    }

    private int retrieveRevisionBase(String fullChangeId) throws Exception {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritPatchSetRevisionsUri(fullChangeId));
        log.debug("Retrieve Revision URI: '{}'", uri);
        JsonObject reviews = forwardGetRequestReturnJsonObject(uri);
        try {
            Set<String> revisions = reviews.get("revisions").getAsJsonObject().keySet();
            return revisions.size() -1;
        }
        catch (Exception e) {
            log.error("Could not retrieve revisions for PatchSet with fullChangeId: {}", fullChangeId, e);
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

    private void filterCommitMessageContent(List<String> fieldValue) {
        fieldValue.removeIf(s ->
                s.isEmpty() || Arrays.stream(COMMIT_MESSAGE_FILTER_OUT_PREFIXES).anyMatch(s::startsWith));
    }

    private void processFileDiffItem(Field inputDiffField, InputFileDiff.Content contentItem,
                                     OutputFileDiff.Content outputContentItem) {
        String diffType = inputDiffField.getName();
        try {
            // Get the `a`, `b` or `ab` field's value from the input diff content
            @SuppressWarnings("unchecked")
            List<String> diffLines = (List<String>) inputDiffField.get(contentItem);
            if (diffLines == null) {
                return;
            }
            if (isCommitMessage) {
                filterCommitMessageContent(diffLines);
            }
            if (config.getGptFullFileReview() || !diffType.equals("ab")) {
                // Get the corresponding `a`, `b` or `ab` field from the output diff class
                Field outputDiffField = OutputFileDiff.Content.class.getDeclaredField(diffType);
                // Store the new field's value in the output diff content `outputContentItem`
                outputDiffField.set(outputContentItem, String.join("\n", diffLines));
            }
            // If the lines modified in the PatchSet are not deleted, they are utilized to populate newFileContent
            if (diffType.contains("b")) {
                newFileContent.addAll(diffLines);
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Error while processing file difference (diff type: {})", diffType, e);
        }
    }

    private void processFileDiff(String filename, String fileDiffJson) {
        log.debug("FileDiff Json processed: {}", fileDiffJson);
        newFileContent = new ArrayList<>() {{
            add("DUMMY LINE #0");
        }};
        InputFileDiff inputFileDiff = gson.fromJson(fileDiffJson, InputFileDiff.class);
        // Initialize the reduced output file diff with fields `meta_a` and `meta_b`
        OutputFileDiff outputFileDiff = new OutputFileDiff(inputFileDiff.getMeta_a(), inputFileDiff.getMeta_b());
        List<OutputFileDiff.Content> outputDiffContent = new ArrayList<>();
        List<InputFileDiff.Content> inputDiffContent = inputFileDiff.getContent();
        // Iterate over the items of the diff content
        for (InputFileDiff.Content inputContentItem : inputDiffContent) {
            OutputFileDiff.Content outputContentItem = new OutputFileDiff.Content();
            // Iterate over the fields `a`, `b` and `ab` of each diff content
            for (Field inputDiffField : InputFileDiff.Content.class.getDeclaredFields()) {
                processFileDiffItem(inputDiffField, inputContentItem, outputContentItem);
            }
            outputDiffContent.add(outputContentItem);
        }
        fileDiffsProcessed.put(filename, new FileDiffProcessed(inputDiffContent, newFileContent));
        outputFileDiff.setContent(outputDiffContent);
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
        return "[" + String.join(",", diffs) + "]\n";
    }

    public String getPatchSet(String fullChangeId, boolean isCommentEvent) throws Exception {
        int revisionBase = isCommentEvent ? 0 : retrieveRevisionBase(fullChangeId);
        log.debug("Revision base: {}", revisionBase);

        List<String> files = getAffectedFiles(fullChangeId, revisionBase);
        log.debug("Patch files: {}", files);

        String fileDiffsJson = getFileDiffsJson(fullChangeId, files, revisionBase);
        log.debug("File diffs: {}", fileDiffsJson);

        return fileDiffsJson;
    }

}
