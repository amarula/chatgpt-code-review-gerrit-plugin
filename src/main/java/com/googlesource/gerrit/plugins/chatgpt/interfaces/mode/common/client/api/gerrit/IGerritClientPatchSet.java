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

package com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

import java.util.HashMap;
import java.util.List;

public interface IGerritClientPatchSet {
  String getPatchSet(ChangeSetData changeSetData, GerritChange gerritChange) throws Exception;

  boolean isDisabledUser(String authorUsername);

  boolean isDisabledTopic(String topic);

  void retrieveRevisionBase(GerritChange change);

  Integer getNotNullAccountId(String authorUsername);

  HashMap<String, FileDiffProcessed> getFileDiffsProcessed();

  List<String> getPatchSetFiles();

  Integer getRevisionBase();
}
