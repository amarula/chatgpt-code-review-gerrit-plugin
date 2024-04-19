package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.UriResourceLocator;

public class UriResourceLocatorStateful extends UriResourceLocator {
    public static String chatCreateFilesUri() {
        return "/v1/files";
    }

    public static String chatCreateAssistantsUri() {
        return "/v1/assistants";
    }

}
