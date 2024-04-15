package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPatchSetDetail;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class GerritClientDetail extends GerritClientBase {
    private GerritPatchSetDetail gerritPatchSetDetail;

    public GerritClientDetail(Configuration config) {
        super(config);
    }

    public List<GerritComment> getMessages(GerritChange change) {
        loadPatchSetDetail(change);
        return gerritPatchSetDetail.getMessages();
    }

    public boolean isWorkInProgress(GerritChange change) {
        loadPatchSetDetail(change);
        return gerritPatchSetDetail.getWorkInProgress() != null && gerritPatchSetDetail.getWorkInProgress();
    }

    public GerritPermittedVotingRange getPermittedVotingRange(GerritChange change) {
        int gptAccountId = ChangeSetDataHandler.getInstance(change).getGptAccountId();
        loadPatchSetDetail(change);
        List<GerritPatchSetDetail.Permission> permissions = gerritPatchSetDetail.getLabels().getCodeReview().getAll();
        if (permissions == null) {
            log.debug("No limitations on the ChatGPT voting range were detected");
            return null;
        }
        for (GerritPatchSetDetail.Permission permission : permissions) {
            if (permission.getAccountId() == gptAccountId) {
                log.debug("PatchSet voting range detected for ChatGPT user: {}", permission.getPermittedVotingRange());
                return permission.getPermittedVotingRange();
            }
        }
        return null;
    }

    private void loadPatchSetDetail(GerritChange change) {
        if (gerritPatchSetDetail != null) {
            return;
        }
        try {
            gerritPatchSetDetail = getReviewDetail(change.getFullChangeId());
        }
        catch (Exception e) {
            log.error("Error retrieving PatchSet details", e);
        }
    }

    private GerritPatchSetDetail getReviewDetail(String fullChangeId) throws Exception {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritGetPatchSetDetailUri(fullChangeId));
        String responseBody = forwardGetRequest(uri);
        return getGson().fromJson(responseBody, GerritPatchSetDetail.class);
    }

}
