package com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

import java.util.HashMap;
import java.util.List;

public interface IGerritClientPatchSet {
    String getPatchSet(ChangeSetData changeSetData, GerritChange gerritChange) throws Exception;
    boolean isDisabledUser(String authorUsername);
    boolean isDisabledTopic(String topic);
    void retrieveRevisionBase(GerritChange change);
    Integer getNotNullAccountId(String authorUsername);
    HashMap<String, FileDiffProcessed> getFileDiffsProcessed();
    List<String> getPatchSetFiles();
    Integer getRevisionBase();
}
