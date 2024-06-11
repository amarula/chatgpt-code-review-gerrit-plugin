package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api;

public class UriResourceLocatorStateful {
    public static String filesCreateUri() {
        return "/v1/files";
    }

    public static String assistantCreateUri() {
        return "/v1/assistants";
    }

    public static String threadsUri() {
        return "/v1/threads";
    }

    public static String threadRetrieveUri(String threadId) {
        return threadsUri() + "/" + threadId;
    }

    public static String threadMessagesUri(String threadId) {
        return threadRetrieveUri(threadId) + "/messages";
    }

    public static String threadMessageRetrieveUri(String threadId, String messageId) {
        return threadMessagesUri(threadId) + "/" + messageId;
    }

    public static String runsUri(String threadId) {
        return threadRetrieveUri(threadId) + "/runs";
    }

    public static String runRetrieveUri(String threadId, String runId) {
        return runsUri(threadId) + "/" + runId;
    }

    public static String runStepsUri(String threadId, String runId) {
        return runRetrieveUri(threadId, runId) + "/steps";
    }
}
