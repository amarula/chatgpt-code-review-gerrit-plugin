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

package com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.api.gerrit;

import com.google.inject.Inject;
import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

@Slf4j
public class GerritClientPatchSetStateless extends GerritClientPatchSet
    implements IGerritClientPatchSet {
  @VisibleForTesting
  @Inject
  public GerritClientPatchSetStateless(Configuration config, AccountCache accountCache) {
    super(config, accountCache);
  }

  public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
    int revisionBase = getChangeSetRevisionBase(changeSetData);
    log.debug("Revision base: {}", revisionBase);

    patchSetFiles = getAffectedFiles(change, revisionBase);
    log.debug("Affected files for Patch Set: {}", patchSetFiles);

    String fileDiffsJson = getFileDiffsJson(change, revisionBase);
    log.debug("File diffs JSON: {}", fileDiffsJson);

    return fileDiffsJson;
  }

  private List<String> getAffectedFiles(GerritChange change, int revisionBase) throws Exception {
    try (ManualRequestContext requestContext = config.openRequestContext()) {
      Map<String, FileInfo> files =
          config
              .getGerritApi()
              .changes()
              .id(
                  change.getProjectName(),
                  change.getBranchNameKey().shortName(),
                  change.getChangeKey().get())
              .current()
              .files(revisionBase);
      return files.entrySet().stream()
          .filter(
              fileEntry -> {
                String filename = fileEntry.getKey();
                if (!filename.equals("/COMMIT_MSG") || config.getGptReviewCommitMessages()) {
                  if (fileEntry.getValue().size > config.getMaxReviewFileSize()) {
                    log.info(
                        "File '{}' not reviewed due to its size exceeding the maximum allowable size"
                            + " of {} bytes.",
                        filename,
                        config.getMaxReviewFileSize());
                    return false;
                  }
                  return true;
                }
                return false;
              })
          .map(Map.Entry::getKey)
          .collect(toList());
    }
  }

  private String getFileDiffsJson(GerritChange change, int revisionBase) throws Exception {
    retrieveFileDiff(change, revisionBase);
    diffs.add(String.format("{\"changeId\": \"%s\"}", change.getFullChangeId()));
    return "[" + String.join(",", diffs) + "]\n";
  }
}
