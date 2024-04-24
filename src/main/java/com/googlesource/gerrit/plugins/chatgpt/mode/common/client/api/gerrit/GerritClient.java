package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;

@Slf4j
@Singleton
public class GerritClient {
    private final GerritClientFacade gerritClientFacade;

    @Inject
    public GerritClient(GerritClientFacade gerritClientFacade) {
        this.gerritClientFacade = gerritClientFacade;
    }

    public GerritPermittedVotingRange getPermittedVotingRange(GerritChange change) {
        return gerritClientFacade.getPermittedVotingRange(change);
    }

    public String getPatchSet(String fullChangeId) throws Exception {
        return getPatchSet(new GerritChange(fullChangeId));
    }

    public String getPatchSet(GerritChange change) throws Exception {
        return gerritClientFacade.getPatchSet(change);
    }

    public boolean isDisabledUser(String authorUsername) {
        return gerritClientFacade.isDisabledUser(authorUsername);
    }

    public boolean isDisabledTopic(String topic) {
        return gerritClientFacade.isDisabledTopic(topic);
    }

    public boolean isWorkInProgress(GerritChange change) {
        return gerritClientFacade.isWorkInProgress(change);
    }

    public HashMap<String, FileDiffProcessed> getFileDiffsProcessed(GerritChange change) {
        return gerritClientFacade.getFileDiffsProcessed();
    }

    public Integer getNotNullAccountId(String authorUsername) {
        return gerritClientFacade.getNotNullAccountId(authorUsername);
    }

    public boolean retrieveLastComments(GerritChange change) {
        return gerritClientFacade.retrieveLastComments(change);
    }

    public void retrievePatchSetInfo(GerritChange change) {
        gerritClientFacade.retrievePatchSetInfo(change);
    }

    public GerritClientData getClientData(GerritChange change) {
        return gerritClientFacade.getClientData(change);
    }
}
