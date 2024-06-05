package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;
import java.nio.file.Path;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptFiles.KEY_FILE_ID;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.createTempFileWithContent;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptAssistant extends ClientBase {
    public static final String KEY_ASSISTANT_ID = "assistantId";

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler pluginDataHandler;

    public ChatGptAssistant(
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandler = pluginDataHandlerProvider.get();
    }

    public void setupAssistant() {
        String assistantId = pluginDataHandler.getValue(KEY_ASSISTANT_ID);
        if (assistantId == null || config.getForceCreateAssistant()) {
            String fileId = uploadRepoFiles();
            pluginDataHandler.setValue(KEY_FILE_ID, fileId);
            assistantId = createAssistant(fileId);
            pluginDataHandler.setValue(KEY_ASSISTANT_ID, assistantId);
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

    private String createAssistant(String fileId) {
        Request request = createRequest(fileId);
        log.debug("ChatGPT Create Assistant request: {}", request);

        ChatGptResponse assistantResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
        log.debug("Assistant created: {}", assistantResponse);

        return assistantResponse.getId();
    }

    private Request createRequest(String fileId) {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.assistantCreateUri());
        log.debug("ChatGPT Create Assistant request URI: {}", uri);
        ChatGptPromptStateful chatGptPromptStateful = new ChatGptPromptStateful(config, change);
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, change.getIsCommentEvent());
        ChatGptTool[] tools = new ChatGptTool[] {
                ChatGptTools.retrieveFormatRepliesTool()
        };
        ChatGptToolResources toolResources = new ChatGptToolResources(new ChatGptFileIds(new String[] {fileId}));
        ChatGptCreateAssistantRequestBody requestBody = ChatGptCreateAssistantRequestBody.builder()
                .name(ChatGptPromptStateful.DEFAULT_GPT_ASSISTANT_NAME)
                .description(chatGptPromptStateful.getDefaultGptAssistantDescription())
                .instructions(chatGptPromptStateful.getDefaultGptAssistantInstructions())
                .model(config.getGptModel())
                .temperature(chatGptParameters.getGptTemperature())
                .tools(tools)
                .toolResources(toolResources)
                .build();

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), requestBody);
    }
}
