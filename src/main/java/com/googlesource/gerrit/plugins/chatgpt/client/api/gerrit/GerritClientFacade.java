package com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritPermittedVotingRange;
import com.googlesource.gerrit.plugins.chatgpt.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class GerritClientFacade {
    private final GerritClientDetail gerritClientDetail;
    private final GerritClientPatchSet gerritClientPatchSet;
    private final GerritClientComments gerritClientComments;

    public GerritClientFacade(Configuration config) {
        gerritClientDetail = new GerritClientDetail(config);
        gerritClientPatchSet = new GerritClientPatchSet(config);
        gerritClientComments = new GerritClientComments(config);
    }

    public GerritPermittedVotingRange getPermittedVotingRange(GerritChange change) {
        return gerritClientDetail.getPermittedVotingRange(change);
    }

    public String getPatchSet(GerritChange change) throws Exception {
        return gerritClientPatchSet.getPatchSet(change);
    }

    public boolean isDisabledUser(String authorUsername) {
        return gerritClientPatchSet.isDisabledUser(authorUsername);
    }

    public boolean isDisabledTopic(String topic) {
        return gerritClientPatchSet.isDisabledTopic(topic);
    }

    public boolean isWorkInProgress(GerritChange change) {
        return gerritClientDetail.isWorkInProgress(change);
    }

    public HashMap<String, FileDiffProcessed> getFileDiffsProcessed() {
        return gerritClientPatchSet.getFileDiffsProcessed();
    }

    public Integer getNotNullAccountId(String authorUsername) {
        return gerritClientPatchSet.getNotNullAccountId(authorUsername);
    }

    public boolean retrieveLastComments(GerritChange change) {
        return gerritClientComments.retrieveLastComments(change);
    }

    public void retrievePatchSetInfo(GerritChange change) {
        gerritClientComments.retrieveAllComments(change);
        gerritClientPatchSet.retrieveRevisionBase(change);
    }

    public GerritClientData getClientData(GerritChange change) {
        return new GerritClientData(
                gerritClientPatchSet.getFileDiffsProcessed(),
                gerritClientDetail.getMessages(change),
                gerritClientComments.getCommentData(),
                gerritClientPatchSet.getRevisionBase()
        );
    }

}
