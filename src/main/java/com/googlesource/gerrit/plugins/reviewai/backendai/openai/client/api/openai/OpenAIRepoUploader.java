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

package com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.backendai.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.openai.endpoint.OpenAIFile;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.backendai.openai.model.api.openai.OpenAIFilesResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.reviewai.utils.FileUtils.createTempFileWithContent;
import static com.googlesource.gerrit.plugins.reviewai.utils.FileUtils.sanitizeFilename;

@Slf4j
public class OpenAIRepoUploader extends ClientBase {
  private static final String FILENAME_PATTERN = "%s_%05d_";

  private final GerritChange change;
  private final GitRepoFiles gitRepoFiles;

  private String filenameBase;
  private int filenameIndex;

  public OpenAIRepoUploader(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
    super(config);
    this.change = change;
    this.gitRepoFiles = gitRepoFiles;
  }

  public List<String> uploadRepoFiles() throws OpenAiConnectionFailException {
    log.debug("Starting uploading repository files.");
    List<String> repoFiles = gitRepoFiles.getGitRepoFilesAsJson(config, change);
    List<String> filesIds = new ArrayList<>();
    filenameBase = sanitizeFilename(change.getProjectName());
    filenameIndex = 0;
    for (String repoFile : repoFiles) {
      Path repoPath = createTempFileWithContent(getIndexedFilename(), ".json", repoFile);
      log.debug("Uploading repository file `{}`", repoPath);
      OpenAIFile openAIFile = new OpenAIFile(config);
      OpenAIFilesResponse openAIFilesResponse = openAIFile.uploadFile(repoPath);
      filesIds.add(openAIFilesResponse.getId());
    }
    log.debug("IDs of the uploaded files: {}", filesIds);

    return filesIds;
  }

  private String getIndexedFilename() {
    filenameIndex++;
    return String.format(FILENAME_PATTERN, filenameBase, filenameIndex);
  }
}
