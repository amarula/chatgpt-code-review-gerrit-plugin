package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;
import java.nio.file.Path;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStore.KEY_VECTOR_STORE_ID;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.createTempFileWithContent;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptAssistantBase extends ClientBase {
    @Getter
    protected String keyAssistantId;

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler projectDataHandler;

    public ChatGptAssistantBase(
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
        this.projectDataHandler = pluginDataHandlerProvider.getProjectScope();
    }

    public void setupAssistant() {
        String assistantId = projectDataHandler.getValue(keyAssistantId);
        if (assistantId == null || config.getForceCreateAssistant()) {
            log.debug("Setup Assistant for project {}", change.getProjectNameKey());
            String fileId = uploadRepoFiles();
            String vectorStoreId = createVectorStore(fileId);
            projectDataHandler.setValue(KEY_VECTOR_STORE_ID, vectorStoreId);
            assistantId = createAssistant(vectorStoreId);
            projectDataHandler.setValue(keyAssistantId, assistantId);
            log.info("Project assistant created with ID: {}", assistantId);
        }
        else {
            log.info("Project assistant found for the project. Assistant ID: {}", assistantId);
        }
    }

    private String uploadRepoFiles() {
        String repoFiles = gitRepoFiles.getGitRepoFiles(change);
        Path repoPath = createTempFileWithContent(change.getProjectName(), ".json", repoFiles);
        ChatGptFiles chatGptFiles = new ChatGptFiles(config);
        ChatGptFilesResponse chatGptFilesResponse = chatGptFiles.uploadFiles(repoPath);

        return chatGptFilesResponse.getId();
    }

    private String createVectorStore(String fileId) {
        ChatGptVectorStore vectorStore = new ChatGptVectorStore(fileId, config, change);
        ChatGptResponse createVectorStoreResponse = vectorStore.createVectorStore();

        return createVectorStoreResponse.getId();
    }

    private String createAssistant(String vectorStoreId) {
        Request request = createRequest(vectorStoreId);
        log.debug("ChatGPT Create Assistant request: {}", request);

        ChatGptResponse assistantResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.debug("Assistant created: {}", assistantResponse);

        return assistantResponse.getId();
    }

    private Request createRequest(String vectorStoreId) {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.assistantCreateUri());
        log.debug("ChatGPT Create Assistant request URI: {}", uri);
        ChatGptPromptStateful chatGptPromptStateful = new ChatGptPromptStateful(config, changeSetData, change);
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, change.getIsCommentEvent());
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
                .name(ChatGptPromptStateful.DEFAULT_GPT_ASSISTANT_NAME)
                .description(chatGptPromptStateful.getDefaultGptAssistantDescription())
                .instructions(chatGptPromptStateful.getDefaultGptAssistantInstructions())
                .model(config.getGptModel())
                .temperature(chatGptParameters.getGptTemperature())
                .tools(tools)
                .toolResources(toolResources)
                .build();
        log.debug("ChatGPT Create Assistant request body: {}", requestBody);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), requestBody);
    }
}
