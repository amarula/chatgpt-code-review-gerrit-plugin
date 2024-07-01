package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit.GerritClientPatchSetHelper.*;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {
    private GerritChange change;

    @VisibleForTesting
    @Inject
    public GerritClientPatchSetStateful(Configuration config, AccountCache accountCache) {
        super(config, accountCache);
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
        if (change.getIsCommentEvent()) return "";
        this.change = change;

        String formattedPatch = getPatchFromGerrit();
        List<String> files = extractFilesFromPatch(formattedPatch);
        retrieveFileDiff(change, files, revisionBase);

        return formattedPatch;
    }

    private String getPatchFromGerrit() throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            String formattedPatch = config
                .getGerritApi()
                .changes()
                .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                .current()
                .patch()
                .asString();
            log.debug("Formatted Patch retrieved: {}", formattedPatch);

            return filterPatch(formattedPatch);
        }
    }

    private String filterPatch(String formattedPatch) {
        if (config.getGptReviewCommitMessages()) {
            return filterPatchWithCommitMessage(formattedPatch);
        }
        else {
            return filterPatchWithoutCommitMessage(change, formattedPatch);
        }
    }
}
