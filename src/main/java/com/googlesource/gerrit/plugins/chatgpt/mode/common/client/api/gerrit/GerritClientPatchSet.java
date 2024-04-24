package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import java.util.Optional;

import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerritClientPatchSet extends GerritClientAccount {
    @Getter
    protected Integer revisionBase = 0;

    public GerritClientPatchSet(Configuration config) {
        super(config);
    }

    public void retrieveRevisionBase(GerritChange change) {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            ChangeInfo changeInfo =
                config
                    .getGerritApi()
                    .changes()
                    .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                    .get(ListChangesOption.ALL_REVISIONS);
            revisionBase =
                Optional.ofNullable(changeInfo.revisions)
                    .map(revisions -> revisions.size() - 1)
                    .orElse(-1);
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
