package com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;

public interface IChatGptClient {
    String ask(Configuration config, String changeId, String patchSet) throws Exception;
    String ask(Configuration config, GerritChange change, String patchSet) throws Exception;
    String getRequestBody();
}
