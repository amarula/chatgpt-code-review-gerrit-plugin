package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.ModeClassLoader;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.gerrit.GerritClientPatchSetStateless;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ClassUtils.registerDynamicClasses;

@Slf4j
public class GerritClientFacade {
    private final ChangeSetData changeSetData;
    private final GerritClientDetail gerritClientDetail;
    private final IGerritClientPatchSet gerritClientPatchSet;
    private final GerritClientComments gerritClientComments;

    @Inject
    public GerritClientFacade(Configuration config, ChangeSetData changeSetData) {
        gerritClientDetail = new GerritClientDetail(config, changeSetData);
        gerritClientPatchSet = (IGerritClientPatchSet) ModeClassLoader.getInstance(
                "client.api.gerrit.GerritClientPatchSet", config, config);
        this.changeSetData = changeSetData;
        registerDynamicClasses(GerritClientPatchSetStateless.class, GerritClientPatchSetStateful.class);
        gerritClientComments = new GerritClientComments(config, changeSetData);
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
