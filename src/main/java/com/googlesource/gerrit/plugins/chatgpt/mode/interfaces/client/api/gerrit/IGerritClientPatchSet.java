package com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;

import java.util.HashMap;

public interface IGerritClientPatchSet {
    String getPatchSet(GerritChange change) throws Exception;
    boolean isDisabledUser(String authorUsername);
    boolean isDisabledTopic(String topic);
    void retrieveRevisionBase(GerritChange change);
    Integer getNotNullAccountId(String authorUsername);
    HashMap<String, FileDiffProcessed> getFileDiffsProcessed();
    Integer getRevisionBase();
}
