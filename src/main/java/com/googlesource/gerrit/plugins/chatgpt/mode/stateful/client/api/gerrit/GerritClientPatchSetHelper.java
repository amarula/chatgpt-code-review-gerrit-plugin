package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.COMMIT_MESSAGE_FILTER_OUT_PREFIXES;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_COMMIT_MESSAGE_PREFIX;

@Slf4j
public class GerritClientPatchSetHelper {
    private static final Pattern EXTRACT_B_FILENAMES_FROM_PATCH_SET = Pattern.compile("^diff --git .*? b/(.*)$",
            Pattern.MULTILINE);

    public static String filterPatchWithCommitMessage(String formattedPatch) {
        // Remove Patch heading up to the Date annotation, so that the commit message is included. Additionally, remove
        // the change type between brackets
        Pattern CONFIG_ID_HEADING_PATTERN = Pattern.compile(
                "^.*?" + GERRIT_COMMIT_MESSAGE_PREFIX + "(?:\\[[^\\]]+\\] )?",
                Pattern.DOTALL
        );
        return CONFIG_ID_HEADING_PATTERN.matcher(formattedPatch).replaceAll(GERRIT_COMMIT_MESSAGE_PREFIX);
    }

    public static String filterPatchWithoutCommitMessage(GerritChange change, String formattedPatch) {
        // Remove Patch heading up to the Change-Id annotation
        Pattern CONFIG_ID_HEADING_PATTERN = Pattern.compile(
                "^.*?" + COMMIT_MESSAGE_FILTER_OUT_PREFIXES.get("CHANGE_ID") + " " + change.getChangeKey().get(),
                Pattern.DOTALL
        );
        return CONFIG_ID_HEADING_PATTERN.matcher(formattedPatch).replaceAll("");
    }

    public static List<String> extractFilesFromPatch(String formattedPatch) {
        Matcher extractFilenameMatcher = EXTRACT_B_FILENAMES_FROM_PATCH_SET.matcher(formattedPatch);
        List<String> files = new ArrayList<>();
        while (extractFilenameMatcher.find()) {
            files.add(extractFilenameMatcher.group(1));
        }
        return files;
    }
}
