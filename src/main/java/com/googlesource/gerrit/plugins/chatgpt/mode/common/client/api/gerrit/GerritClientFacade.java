package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerritClientFacade {
    private final ChangeSetData changeSetData;
    private final GerritClientDetail gerritClientDetail;
    private final GerritClientComments gerritClientComments;
    private final IGerritClientPatchSet gerritClientPatchSet;

    @VisibleForTesting
    @Inject
    public GerritClientFacade(
            Configuration config,
            ChangeSetData changeSetData,
            GerritClientComments gerritClientComments,
            IGerritClientPatchSet gerritClientPatchSet) {
        gerritClientDetail = new GerritClientDetail(config, changeSetData);
        this.gerritClientPatchSet = gerritClientPatchSet;
        this.changeSetData = changeSetData;
        this.gerritClientComments = gerritClientComments;
    }

    public GerritPermittedVotingRange getPermittedVotingRange(GerritChange change) {
        return gerritClientDetail.getPermittedVotingRange(change);
    }

    public String getPatchSet(GerritChange change) throws Exception {
        return gerritClientPatchSet.getPatchSet(changeSetData, change);
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
                gerritClientPatchSet,
                gerritClientDetail.getMessages(change),
                gerritClientComments.getCommentData(),
                gerritClientPatchSet.getRevisionBase()
        );
    }
}
