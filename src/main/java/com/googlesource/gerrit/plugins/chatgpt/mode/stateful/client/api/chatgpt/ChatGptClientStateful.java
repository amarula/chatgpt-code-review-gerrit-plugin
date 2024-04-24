package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt.IChatGptClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ChatGptClientStateful implements IChatGptClient {
    @Override
    public String ask(Configuration config, ChangeSetData changeSetData, String changeId, String patchSet) throws Exception {
        // Placeholder implementation, change to actual logic later.
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public String ask(Configuration config, ChangeSetData changeSetData, GerritChange change, String patchSet) throws Exception {
        // Placeholder implementation, change to actual logic later.
        throw new UnsupportedOperationException("Method not implemented yet.");
    }

    @Override
    public String getRequestBody() {
        // Placeholder implementation, change to actual logic later.
        return "Method not implemented yet.";
    }

}
