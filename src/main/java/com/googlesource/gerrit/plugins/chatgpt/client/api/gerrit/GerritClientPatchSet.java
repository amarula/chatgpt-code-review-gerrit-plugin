/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClientPatchSetHelper.*;

@Slf4j
public class GerritClientPatchSet extends GerritClientPatchSetImpl implements IGerritClientPatchSet {
  private GerritChange change;

  @VisibleForTesting
  @Inject
  public GerritClientPatchSet(Configuration config, AccountCache accountCache) {
    super(config, accountCache);
  }

  public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
    if (change.getIsCommentEvent()) {
      log.debug("No patch set retrieval because the change is a comment event.");
      return "";
    }
    this.change = change;

    String formattedPatch = getPatchFromGerrit();
    patchSetFiles = extractFilesFromPatch(formattedPatch);
    log.debug("Files extracted from patch: {}", patchSetFiles);
    retrieveFileDiff(change, revisionBase);

    return formattedPatch;
  }

  private String getPatchFromGerrit() throws Exception {
    try (ManualRequestContext requestContext = config.openRequestContext()) {
      String formattedPatch =
          config
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
      String patchWithCommitMessage = filterPatchWithCommitMessage(formattedPatch);
      log.debug("Patch filtered to include commit messages: {}", patchWithCommitMessage);
      return patchWithCommitMessage;
    } else {
      String patchWithoutCommitMessage = filterPatchWithoutCommitMessage(change, formattedPatch);
      log.debug("Patch filtered to exclude commit messages: {}", patchWithoutCommitMessage);
      return patchWithoutCommitMessage;
    }
  }
}
