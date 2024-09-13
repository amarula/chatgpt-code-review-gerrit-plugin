package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
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

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStore.KEY_VECTOR_STORE_ID;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils.now;

@Slf4j
public class ChatGptAssistant extends ClientBase {
    private static final String ASSISTANT_ID_LOG = "assistantIdLog";

    private final ChatGptHttpClient httpClient;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final PluginDataHandler projectDataHandler;
    private final PluginDataHandler changeDataHandler;
    private final PluginDataHandler assistantsDataHandler;
    private final ChatGptRepoUploader chatGptRepoUploader;

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
        httpClient = new ChatGptHttpClient(config);
        projectDataHandler = pluginDataHandlerProvider.getProjectScope();
        changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        assistantsDataHandler = pluginDataHandlerProvider.getAssistantsWorkspace();
        chatGptRepoUploader = new ChatGptRepoUploader(config, change, gitRepoFiles);
        log.debug("Initialized ChatGptAssistant with project and assistants data handlers.");
    }

    public String setupAssistant() throws OpenAiConnectionFailException {
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
        changeDataHandler.appendJsonValue(ASSISTANT_ID_LOG, now() + ": " + assistantId, String.class);

        return assistantId;
    }

    public String createVectorStore() throws OpenAiConnectionFailException {
        log.debug("Creating or retrieving vector store.");
        String vectorStoreId = projectDataHandler.getValue(KEY_VECTOR_STORE_ID);
        if (vectorStoreId == null) {
            List<String> fileIds = chatGptRepoUploader.uploadRepoFiles();
            ChatGptVectorStore vectorStore = new ChatGptVectorStore(fileIds, config, change);
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

    public void flushAssistantAndVectorIds() {
        log.debug("Flushing assistant IDs.");
        projectDataHandler.removeValue(KEY_VECTOR_STORE_ID);
        assistantsDataHandler.destroy();
    }

    private String createAssistant(String vectorStoreId) throws OpenAiConnectionFailException {
        log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
        Request request = createRequest(vectorStoreId);
        log.debug("ChatGPT Create Assistant request: {}", request);

        ChatGptResponse assistantResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.debug("Assistant created: {}", assistantResponse);

        return assistantResponse.getId();
    }

    private Request createRequest(String vectorStoreId) {
        log.debug("Creating request to build new assistant.");
        String uri = UriResourceLocatorStateful.assistantCreateUri();
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
        return httpClient.createRequestFromJson(uri, requestBody);
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
