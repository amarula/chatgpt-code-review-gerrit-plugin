package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gson.JsonObject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.UriResourceLocator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.*;

@Slf4j
public class GerritClientPatchSet extends GerritClientAccount {
    @Getter
    protected Integer revisionBase = 0;

    public GerritClientPatchSet(Configuration config) {
        super(config);
    }

    public void retrieveRevisionBase(GerritChange change) {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritPatchSetRevisionsUri(change.getFullChangeId()));
        log.debug("Retrieve Revision URI: '{}'", uri);
        try {
            JsonObject reviews = forwardGetRequestReturnJsonObject(uri);
            Set<String> revisions = reviews.get("revisions").getAsJsonObject().keySet();
            revisionBase = revisions.size() -1;
        }
        catch (Exception e) {
            log.error("Could not retrieve revisions for PatchSet with fullChangeId: {}", change.getFullChangeId(), e);
            revisionBase = 0;
        }
    }

    protected int getChangeSetRevisionBase(GerritChange change) {
        return isChangeSetBased(change) ? 0 : revisionBase;
    }

    private boolean isChangeSetBased(GerritChange change) {
        return !ChangeSetDataHandler.getInstance(change).getForcedReviewLastPatchSet();
    }

}
