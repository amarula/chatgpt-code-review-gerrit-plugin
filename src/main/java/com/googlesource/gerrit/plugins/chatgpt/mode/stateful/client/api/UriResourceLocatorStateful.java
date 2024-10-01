package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api;

public class UriResourceLocatorStateful {
    private static final String VERSION_URI = "/v1";

    public static String filesCreateUri() {
        return VERSION_URI + "/files";
    }

    public static String assistantCreateUri() {
        return VERSION_URI + "/assistants";
    }

    public static String threadsUri() {
        return VERSION_URI + "/threads";
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

    public static String runCancelUri(String threadId, String runId) {
        return runRetrieveUri(threadId, runId) + "/cancel";
    }

    public static String runSubmitToolOutputsUri(String threadId, String runId) {
        return runRetrieveUri(threadId, runId) + "/submit_tool_outputs";
    }

    public static String vectorStoreCreateUri() {
        return VERSION_URI + "/vector_stores";
    }

    public static String vectorStoreFileBatchCreateUri(String vectorStoreId) {
        return vectorStoreCreateUri() + "/" + vectorStoreId + "/file_batches";
    }

    public static String vectorStoreFileBatchRetrieveUri(String vectorStoreId, String vectorStoreFileBatchId) {
        return vectorStoreFileBatchCreateUri(vectorStoreId) + "/" + vectorStoreFileBatchId;
    }
}
