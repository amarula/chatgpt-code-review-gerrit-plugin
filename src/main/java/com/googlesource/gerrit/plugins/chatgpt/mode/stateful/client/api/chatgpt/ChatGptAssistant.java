package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import com.googlesource.gerrit.plugins.chatgpt.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStore.KEY_VECTOR_STORE_ID;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.createTempFileWithContent;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.sanitizeFilename;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptAssistant extends ClientBase {
    private static final String KEY_LAST_ASSISTANT_ID = "lastAssistantId";

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler projectDataHandler;
    private final PluginDataHandler changeDataHandler;
    private final PluginDataHandler assistantsDataHandler;

    private String description;
    private String instructions;
    private String model;
    private Double temperature;

    public ChatGptAssistant(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        projectDataHandler = pluginDataHandlerProvider.getProjectScope();
        changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        assistantsDataHandler = pluginDataHandlerProvider.getAssistantsWorkspace();
        log.debug("Initialized ChatGptAssistant with project and assistants data handlers.");
    }

    public String setupAssistant() {
        log.debug("Setting up the assistant parameters.");
        setupAssistantParameters();
        String assistantIdHashKey = calculateAssistantIdHashKey();
        log.info("Calculated assistant id hash key: {}", assistantIdHashKey);
        String assistantId = assistantsDataHandler.getValue(assistantIdHashKey);
        if (assistantId == null || config.getForceCreateAssistant()) {
            log.debug("Setup Assistant for project {}", change.getProjectNameKey());
            String vectorStoreId = createVectorStore();
            assistantId = createAssistant(vectorStoreId);
            assistantsDataHandler.setValue(assistantIdHashKey, assistantId);
            log.info("Project assistant created with ID: {}", assistantId);
        }
        else {
            log.info("Project assistant found for the project. Assistant ID: {}", assistantId);
        }
        changeDataHandler.setValue(KEY_LAST_ASSISTANT_ID, assistantId);
        return assistantId;
    }

    public String createVectorStore() {
        log.debug("Creating or retrieving vector store.");
        String vectorStoreId = projectDataHandler.getValue(KEY_VECTOR_STORE_ID);
        if (vectorStoreId == null) {
            String fileId = uploadRepoFiles();
            ChatGptVectorStore vectorStore = new ChatGptVectorStore(fileId, config, change);
            ChatGptResponse createVectorStoreResponse = vectorStore.createVectorStore();
            vectorStoreId = createVectorStoreResponse.getId();
            projectDataHandler.setValue(KEY_VECTOR_STORE_ID, vectorStoreId);
            log.info("Vector Store created with ID: {}", vectorStoreId);
        }
        else {
            log.info("Vector Store found for the project. Vector Store ID: {}", vectorStoreId);
        }
        return vectorStoreId;
    }

    public void flushAssistantIds() {
        log.debug("Flushing assistant IDs.");
        projectDataHandler.removeValue(KEY_VECTOR_STORE_ID);
        assistantsDataHandler.destroy();
    }

    private String uploadRepoFiles() {
        log.debug("Uploading repository files.");
        String repoFiles = gitRepoFiles.getGitRepoFiles(config, change);
        Path repoPath = createTempFileWithContent(sanitizeFilename(change.getProjectName()), ".json", repoFiles);
        ChatGptFiles chatGptFiles = new ChatGptFiles(config);
        ChatGptFilesResponse chatGptFilesResponse = chatGptFiles.uploadFiles(repoPath);

        return chatGptFilesResponse.getId();
    }

    private String createAssistant(String vectorStoreId) {
        log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
        Request request = createRequest(vectorStoreId);
        log.debug("ChatGPT Create Assistant request: {}", request);

        ChatGptResponse assistantResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.debug("Assistant created: {}", assistantResponse);

        return assistantResponse.getId();
    }

    private Request createRequest(String vectorStoreId) {
        log.debug("Creating request to build new assistant.");
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.assistantCreateUri());
        log.debug("ChatGPT Create Assistant request URI: {}", uri);
        ChatGptTool[] tools = new ChatGptTool[] {
                new ChatGptTool("file_search"),
                ChatGptTools.retrieveFormatRepliesTool()
        };
        ChatGptToolResources toolResources = new ChatGptToolResources(
                new ChatGptToolResources.VectorStoreIds(
                        new String[] {vectorStoreId}
                )
        );
        ChatGptCreateAssistantRequestBody requestBody = ChatGptCreateAssistantRequestBody.builder()
                .name(ChatGptPromptStatefulBase.DEFAULT_GPT_ASSISTANT_NAME)
                .description(description)
                .instructions(instructions)
                .model(model)
                .temperature(temperature)
                .tools(tools)
                .toolResources(toolResources)
                .build();
        log.debug("Request body for creating assistant: {}", requestBody);
        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), requestBody);
    }

    private void setupAssistantParameters() {
        log.debug("Setting up assistant parameters based on current configuration and change set data.");
        IChatGptPromptStateful chatGptPromptStateful = getChatGptPromptStateful(config, changeSetData, change);
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, change.getIsCommentEvent());

        description = chatGptPromptStateful.getDefaultGptAssistantDescription();
        instructions = chatGptPromptStateful.getDefaultGptAssistantInstructions();
        model = config.getGptModel();
        temperature = chatGptParameters.getGptTemperature();
    }

    private String calculateAssistantIdHashKey() {
        log.debug("Calculating hash key for assistant ID.");
        return HashUtils.hashData(new ArrayList<>(List.of(
                description,
                instructions,
                model,
                temperature.toString()
        )));
    }
}
