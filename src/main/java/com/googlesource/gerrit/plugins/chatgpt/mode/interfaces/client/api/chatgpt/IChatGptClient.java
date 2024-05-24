package com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

public interface IChatGptClient {
    ChatGptResponseContent ask(Configuration config, ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws Exception;
    String getRequestBody();
}
