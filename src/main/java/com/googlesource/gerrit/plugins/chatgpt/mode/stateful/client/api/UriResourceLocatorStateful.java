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
}
