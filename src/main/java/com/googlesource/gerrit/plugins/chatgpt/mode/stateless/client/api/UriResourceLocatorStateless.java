package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.UriResourceLocator;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UriResourceLocatorStateless extends UriResourceLocator {
    public static String gerritDiffPostfixUri(String filename) {
        return "/" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "/diff";
    }

    public static String gerritPatchSetFilesUri(String fullChangeId) {
        return gerritSetChangesUri(fullChangeId, "/revisions/current/files");
    }

    public static String chatCompletionsUri() {
        return "/v1/chat/completions";
    }

}
