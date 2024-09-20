package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptVectorStore;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptVectorStoreFileBatch;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ChatGptVectorStoreHandler extends ClientBase {
    private static final int MAX_VECTOR_STORE_GENERATION_RETRIES = 3;
    private static final String KEY_VECTOR_STORE_ID = "vectorStoreId";
    private static final String KEY_VECTOR_STORE_FILE_BATCH_ID = "vectorStoreFileBatchId";
    private static final String KEY_VECTOR_STORE_FILE_BATCH_STATUS = "vectorStoreFileBatchStatus";

    private final GerritChange change;
    private final PluginDataHandler projectDataHandler;
    private final ChatGptRepoUploader chatGptRepoUploader;
    private final ChatGptPoller chatGptPoller;

    public ChatGptVectorStoreHandler(
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandler projectDataHandler
    ) {
        super(config);
        this.change = change;
        this.projectDataHandler = projectDataHandler;
        chatGptRepoUploader = new ChatGptRepoUploader(config, change, gitRepoFiles);
        chatGptPoller = new ChatGptPoller(config);
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
        ChatGptVectorStore vectorStore = new ChatGptVectorStore(config, change);
        ChatGptResponse createVectorStoreResponse = vectorStore.createVectorStore();
        String vectorStoreId = createVectorStoreResponse.getId();
        projectDataHandler.setValue(KEY_VECTOR_STORE_ID, vectorStoreId);
        log.info("Empty Vector Store created with ID: {}", vectorStoreId);

        return vectorStoreId;
    }

    private void createVectorStoreFileBatch(String vectorStoreId, List<String> fileIds) throws OpenAiConnectionFailException {
        log.debug("Creating Vector Store File Batch.");
        ChatGptVectorStoreFileBatch vectorStoreFileBatch = new ChatGptVectorStoreFileBatch(config);
        ChatGptResponse createVectorStoreFileBatchResponse = vectorStoreFileBatch.createVectorStoreFileBatch(
                vectorStoreId,
                fileIds
        );
        String vectorStoreFileBatchId = createVectorStoreFileBatchResponse.getId();
        projectDataHandler.setValue(KEY_VECTOR_STORE_FILE_BATCH_ID, vectorStoreFileBatchId);
        updateVectorStoreFileBatchStatus(createVectorStoreFileBatchResponse);
        log.info("Vector Store File Batch created with ID: {}, Status: {}", vectorStoreFileBatchId,
                createVectorStoreFileBatchResponse.getStatus());
    }

    private String createVectorStore() throws OpenAiConnectionFailException {
        log.debug("Creating new vector store.");
        List<String> fileIds = chatGptRepoUploader.uploadRepoFiles();
        String vectorStoreId = createEmptyVectorStore();
        createVectorStoreFileBatch(vectorStoreId, fileIds);

        return vectorStoreId;
    }

    private boolean checkVectorStoreStatus(String vectorStoreId) throws OpenAiConnectionFailException {
        String vectorStoreFileBatchStatus = projectDataHandler.getValue(KEY_VECTOR_STORE_FILE_BATCH_STATUS);
        if (chatGptPoller.isNotCompleted(vectorStoreFileBatchStatus)) {
            String vectorStoreFileBatchId = projectDataHandler.getValue(KEY_VECTOR_STORE_FILE_BATCH_ID);
            ChatGptResponse vectorStoreFileBatchResponse = chatGptPoller.runPoll(
                    UriResourceLocatorStateful.vectorStoreFileBatchRetrieveUri(vectorStoreId, vectorStoreFileBatchId),
                    new ChatGptResponse()
            );
            updateVectorStoreFileBatchStatus(vectorStoreFileBatchResponse);
            log.info("Vector Store File Batch poll executed after {} seconds ({} polling requests); Response: {}",
                    chatGptPoller.getElapsedTime(), chatGptPoller.getPollingCount(), vectorStoreFileBatchResponse);
            return vectorStoreFileBatchResponse.getStatus().equals(ChatGptPoller.COMPLETED_STATUS);
        }
        return true;
    }

    private void updateVectorStoreFileBatchStatus(ChatGptResponse vectorStoreFileBatchResponse) {
        projectDataHandler.setValue(KEY_VECTOR_STORE_FILE_BATCH_STATUS, vectorStoreFileBatchResponse.getStatus());
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
