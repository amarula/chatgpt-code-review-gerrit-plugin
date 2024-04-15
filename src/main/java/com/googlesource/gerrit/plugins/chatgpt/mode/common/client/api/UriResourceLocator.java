package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class UriResourceLocator {
    private static final String AUTH_PREFIX_URI = "/a";

    public static String gerritAccountsUri() {
        return AUTH_PREFIX_URI + "/accounts";
    }

    public static String gerritAccountIdUri(String userName) {
        return gerritAccountsUri() + "/?q=username:" + URLEncoder.encode(userName, StandardCharsets.UTF_8);
    }

    public static String gerritGroupPostfixUri(int accountId) {
        return "/" + accountId + "/groups";
    }

    public static String gerritRevisionBasePostfixUri(int revisionBase) {
        return revisionBase > 0 ? "/?base=" + revisionBase : "";
    }

    public static String gerritPatchSetRevisionsUri(String fullChangeId) {
        return gerritSetChangesUri(fullChangeId, "/?o=ALL_REVISIONS");
    }

    public static String gerritGetAllPatchSetCommentsUri(String fullChangeId) {
        return gerritSetChangesUri(fullChangeId, "/comments");
    }

    public static String gerritSetReviewUri(String fullChangeId) {
        return gerritSetChangesUri(fullChangeId, "/revisions/current/review");
    }

    public static String gerritGetPatchSetDetailUri(String fullChangeId) {
        return gerritSetChangesUri(fullChangeId, "/detail");
    }

    protected static String gerritSetChangesUri(String fullChangeId, String uriPostfix) {
        return AUTH_PREFIX_URI + "/changes/" + fullChangeId + uriPostfix;
    }

}
