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

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.ClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.OpenAIUriResourceLocator;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAIVectorStore;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.openai.endpoint.OpenAIVectorStoreFileBatch;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIResponse;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIRunResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class OpenAIVectorStoreHandler extends ClientBase {
  private static final int MAX_VECTOR_STORE_GENERATION_RETRIES = 3;
  private static final String KEY_VECTOR_STORE_ID = "vectorStoreId";
  private static final String KEY_VECTOR_STORE_FILE_BATCH_ID = "vectorStoreFileBatchId";
  private static final String KEY_VECTOR_STORE_FILE_BATCH_STATUS = "vectorStoreFileBatchStatus";

  private final GerritChange change;
  private final PluginDataHandler projectDataHandler;
  private final OpenAIRepoUploader openAIRepoUploader;
  private final OpenAIPoller openAIPoller;

  public OpenAIVectorStoreHandler(
      Configuration config,
      GerritChange change,
      GitRepoFiles gitRepoFiles,
      PluginDataHandler projectDataHandler) {
    super(config);
    this.change = change;
    this.projectDataHandler = projectDataHandler;
    openAIRepoUploader = new OpenAIRepoUploader(config, change, gitRepoFiles);
    openAIPoller = new OpenAIPoller(config);
  }

  public String generateVectorStore() throws OpenAiConnectionFailException {
    log.debug("Creating or retrieving vector store.");
    String vectorStoreId = projectDataHandler.getValue(KEY_VECTOR_STORE_ID);
    if (vectorStoreId == null) {
      vectorStoreId = createVectorStore();
    }
    return validateVectorStore(vectorStoreId);
  }

  public void removeVectorStoreId() {
    projectDataHandler.removeValue(KEY_VECTOR_STORE_ID);
    projectDataHandler.removeValue(KEY_VECTOR_STORE_FILE_BATCH_ID);
    projectDataHandler.removeValue(KEY_VECTOR_STORE_FILE_BATCH_STATUS);
  }

  private String createEmptyVectorStore() throws OpenAiConnectionFailException {
    log.debug("Creating empty Vector Store.");
    OpenAIVectorStore vectorStore = new OpenAIVectorStore(config, change);
    OpenAIResponse createVectorStoreResponse = vectorStore.createVectorStore();
    String vectorStoreId = createVectorStoreResponse.getId();
    projectDataHandler.setValue(KEY_VECTOR_STORE_ID, vectorStoreId);
    log.info("Empty Vector Store created with ID: {}", vectorStoreId);

    return vectorStoreId;
  }

  private void createVectorStoreFileBatch(String vectorStoreId, List<String> fileIds)
      throws OpenAiConnectionFailException {
    log.debug("Creating Vector Store File Batch.");
    OpenAIVectorStoreFileBatch vectorStoreFileBatch = new OpenAIVectorStoreFileBatch(config);
    OpenAIResponse createVectorStoreFileBatchResponse =
        vectorStoreFileBatch.createVectorStoreFileBatch(vectorStoreId, fileIds);
    String vectorStoreFileBatchId = createVectorStoreFileBatchResponse.getId();
    projectDataHandler.setValue(KEY_VECTOR_STORE_FILE_BATCH_ID, vectorStoreFileBatchId);
    updateVectorStoreFileBatchStatus(createVectorStoreFileBatchResponse);
    log.info(
        "Vector Store File Batch created with ID: {}, Status: {}",
        vectorStoreFileBatchId,
        createVectorStoreFileBatchResponse.getStatus());
  }

  private String createVectorStore() throws OpenAiConnectionFailException {
    log.debug("Creating new vector store.");
    List<String> fileIds = openAIRepoUploader.uploadRepoFiles();
    String vectorStoreId = createEmptyVectorStore();
    createVectorStoreFileBatch(vectorStoreId, fileIds);

    return vectorStoreId;
  }

  private boolean checkVectorStoreStatus(String vectorStoreId)
      throws OpenAiConnectionFailException {
    String vectorStoreFileBatchStatus =
        projectDataHandler.getValue(KEY_VECTOR_STORE_FILE_BATCH_STATUS);
    if (OpenAIPoller.isNotCompleted(vectorStoreFileBatchStatus)) {
      String vectorStoreFileBatchId = projectDataHandler.getValue(KEY_VECTOR_STORE_FILE_BATCH_ID);
      OpenAIResponse vectorStoreFileBatchResponse =
          openAIPoller.runPoll(
              OpenAIUriResourceLocator.vectorStoreFileBatchRetrieveUri(
                  vectorStoreId, vectorStoreFileBatchId),
              new OpenAIRunResponse());
      updateVectorStoreFileBatchStatus(vectorStoreFileBatchResponse);
      log.info(
          "Vector Store File Batch poll executed after {} seconds ({} polling requests); Response: {}",
          openAIPoller.getElapsedTime(),
          openAIPoller.getPollingCount(),
          vectorStoreFileBatchResponse);
      return vectorStoreFileBatchResponse.getStatus().equals(OpenAIPoller.COMPLETED_STATUS);
    }
    return true;
  }

  private void updateVectorStoreFileBatchStatus(OpenAIResponse vectorStoreFileBatchResponse) {
    projectDataHandler.setValue(
        KEY_VECTOR_STORE_FILE_BATCH_STATUS, vectorStoreFileBatchResponse.getStatus());
  }

  private String validateVectorStore(String vectorStoreId) throws OpenAiConnectionFailException {
    int retries = 0;
    while (true) {
      retries++;
      log.debug("Validating Vector Store - Attempt #{}", retries);
      if (checkVectorStoreStatus(vectorStoreId)) {
        log.info("Vector Store found for the project. Vector Store ID: {}", vectorStoreId);
        return vectorStoreId;
      }
      log.info("Error validating Vector Store");
      if (retries == MAX_VECTOR_STORE_GENERATION_RETRIES) {
        break;
      }
      vectorStoreId = createVectorStore();
    }
    throw new OpenAiConnectionFailException("Error calculating Vector Store");
  }
}
