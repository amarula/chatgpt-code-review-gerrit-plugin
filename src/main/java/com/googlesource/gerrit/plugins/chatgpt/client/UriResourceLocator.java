package com.googlesource.gerrit.plugins.chatgpt.client;

public class UriResourceLocator {

    private UriResourceLocator() {
        throw new IllegalStateException("Utility class");
    }

    private static String gerritAuthPrefixUri() {
        return "/a";
    }

    public static String gerritPatchSetUri(String fullChangeId) {
        return "/changes/" + fullChangeId + "/revisions/current/patch";
    }

    public static String gerritCommentUri(String fullChangeId) {
        return gerritAuthPrefixUri() + "/changes/" + fullChangeId + "/revisions/current/review";
    }

    public static String chatCompletionsUri() {
        return "/v1/chat/completions";
    }

}
