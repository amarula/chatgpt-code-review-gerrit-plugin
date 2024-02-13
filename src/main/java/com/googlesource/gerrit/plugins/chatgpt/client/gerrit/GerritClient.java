package com.googlesource.gerrit.plugins.chatgpt.client.gerrit;

import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.client.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.client.model.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.client.model.ReviewBatch;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.utils.SingletonManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Singleton
public class GerritClient {
    private static final String DEFAULT_CHANGE_ID = "DEFAULT_CHANGE_ID";
    private static GerritClientFacade gerritClientFacade;

    public void initialize(Configuration config) {
        initialize(config, new GerritChange(DEFAULT_CHANGE_ID));
    }

    public void initialize(Configuration config, GerritChange change) {
        log.debug("Initializing client instances for change: {}", change.getFullChangeId());
        gerritClientFacade = SingletonManager.getInstance(GerritClientFacade.class, change, config);
    }

    public String getPatchSet(String fullChangeId) throws Exception {
        return getPatchSet(new GerritChange(fullChangeId));
    }

    public String getPatchSet(GerritChange change) throws Exception {
        updateGerritClientFacade(change);
        return gerritClientFacade.getPatchSet(change);
    }

    public boolean isDisabledUser(String authorUsername) {
        return gerritClientFacade.isDisabledUser(authorUsername);
    }

    public boolean isDisabledTopic(String topic) {
        return gerritClientFacade.isDisabledTopic(topic);
    }

    public HashMap<String, FileDiffProcessed> getFileDiffsProcessed(GerritChange change) {
        updateGerritClientFacade(change);
        return gerritClientFacade.getFileDiffsProcessed();
    }

    public Integer getNotNullAccountId(GerritChange change, String authorUsername) {
        updateGerritClientFacade(change);
        return gerritClientFacade.getNotNullAccountId(authorUsername);
    }

    public List<GerritComment> getCommentProperties(GerritChange change) {
        updateGerritClientFacade(change);
        return gerritClientFacade.getCommentProperties();
    }

    public void setReview(String fullChangeId, List<ReviewBatch> reviewBatches) throws Exception {
        setReview(new GerritChange(fullChangeId), reviewBatches, null);
    }

    public void setReview(GerritChange change, List<ReviewBatch> reviewBatches, Integer reviewScore) throws Exception {
        updateGerritClientFacade(change);
        gerritClientFacade.setReview(change.getFullChangeId(), reviewBatches, reviewScore);
    }

    public boolean retrieveLastComments(GerritChange change) {
        updateGerritClientFacade(change);
        return gerritClientFacade.retrieveLastComments(change);
    }

    public String getUserRequests(GerritChange change) {
        updateGerritClientFacade(change);
        return gerritClientFacade.getUserRequests(change);
    }

    public void destroy(GerritChange change) {
        log.debug("Destroying GerritClientFacade instance for change: {}", change.getFullChangeId());
        SingletonManager.removeInstance(GerritClientFacade.class, change.getFullChangeId());
    }

    private void updateGerritClientFacade(GerritChange change) {
        gerritClientFacade = SingletonManager.getInstance(GerritClientFacade.class, change);
    }

}
