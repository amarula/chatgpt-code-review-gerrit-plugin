package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_COMMIT_MESSAGE_PREFIX;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.COMMIT_MESSAGE_FILTER_OUT_PREFIXES;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {
    private static final Pattern EXTRACT_B_FILENAMES_FROM_PATCH_SET = Pattern.compile("^diff --git .*? b/(.*)$",
            Pattern.MULTILINE);

    private GerritChange change;

    @VisibleForTesting
    @Inject
    public GerritClientPatchSetStateful(Configuration config, AccountCache accountCache) {
        super(config, accountCache);
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
        if (change.getIsCommentEvent()) return "";
        this.change = change;

        String formattedPatch = getPatchFromGerrit();
        List<String> files = extractFilesFromPatch(formattedPatch);
        retrieveFileDiff(change, files, revisionBase);

        return formattedPatch;
    }

    private String getPatchFromGerrit() throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            String formattedPatch = config
                .getGerritApi()
                .changes()
                .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                .current()
                .patch()
                .asString();
            log.debug("Formatted Patch retrieved: {}", formattedPatch);

            return filterPatch(formattedPatch);
        }
    }

    private String filterPatch(String formattedPatch) {
        if (config.getGptReviewCommitMessages()) {
            return filterPatchWithCommitMessage(formattedPatch);
        }
        else {
            return filterPatchWithoutCommitMessage(formattedPatch);
        }
    }

    private String filterPatchWithCommitMessage(String formattedPatch) {
        // Remove Patch heading up to the Date annotation, so that the commit message is included. Additionally, remove
        // the change type between brackets
        Pattern CONFIG_ID_HEADING_PATTERN = Pattern.compile(
                "^.*?" + GERRIT_COMMIT_MESSAGE_PREFIX + "(?:\\[[^\\]]+\\] )?",
                Pattern.DOTALL
        );
        return CONFIG_ID_HEADING_PATTERN.matcher(formattedPatch).replaceAll(GERRIT_COMMIT_MESSAGE_PREFIX);
    }

    private String filterPatchWithoutCommitMessage(String formattedPatch) {
        // Remove Patch heading up to the Change-Id annotation
        Pattern CONFIG_ID_HEADING_PATTERN = Pattern.compile(
                "^.*?" + COMMIT_MESSAGE_FILTER_OUT_PREFIXES.get("CHANGE_ID") + " " + change.getChangeKey().get(),
                Pattern.DOTALL
        );
        return CONFIG_ID_HEADING_PATTERN.matcher(formattedPatch).replaceAll("");
    }

    private List<String> extractFilesFromPatch(String formattedPatch) {
        Matcher extractFilenameMatcher = EXTRACT_B_FILENAMES_FROM_PATCH_SET.matcher(formattedPatch);
        List<String> files = new ArrayList<>();
        while (extractFilenameMatcher.find()) {
            files.add(extractFilenameMatcher.group(1));
        }
        return files;
    }
}
