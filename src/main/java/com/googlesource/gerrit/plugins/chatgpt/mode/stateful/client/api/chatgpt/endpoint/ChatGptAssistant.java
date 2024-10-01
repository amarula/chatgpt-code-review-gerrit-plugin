package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptTools;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptApiBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt.ChatGptPromptStatefulBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;

@Slf4j
public class ChatGptAssistant extends ChatGptApiBase {
    @Getter
    private final String description;
    @Getter
    private final String instructions;
    @Getter
    private final String model;
    @Getter
    private final Double temperature;

    private ChatGptToolResources toolResources = null;
    private List<ChatGptTool> tools;

    public ChatGptAssistant(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config);
        log.debug("Setting up assistant parameters based on current configuration and change set data.");
        IChatGptPromptStateful chatGptPromptStateful = getChatGptPromptStateful(config, changeSetData, change);
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, change.getIsCommentEvent());
        description = chatGptPromptStateful.getDefaultGptAssistantDescription();
        instructions = chatGptPromptStateful.getDefaultGptAssistantInstructions();
        model = config.getGptModel();
        temperature = chatGptParameters.getGptTemperature();
    }

    public String createAssistant(String vectorStoreId) throws OpenAiConnectionFailException {
        log.debug("Creating assistant with vector store ID: {}", vectorStoreId);
        Request request = createRequest(vectorStoreId);
        log.debug("ChatGPT Create Assistant request: {}", request);

        ChatGptResponse assistantResponse = getChatGptResponse(request);
        log.debug("Assistant created: {}", assistantResponse);

        return assistantResponse.getId();
    }

    private Request createRequest(String vectorStoreId) {
        log.debug("Creating request to build new assistant.");
        String uri = UriResourceLocatorStateful.assistantCreateUri();
        log.debug("ChatGPT Create Assistant request URI: {}", uri);
        updateTools(vectorStoreId);

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

    private void updateTools(String vectorStoreId) {
        ChatGptTools chatGptFormatRepliesTools = new ChatGptTools(ChatGptTools.Functions.formatReplies);
        tools = new ArrayList<>(List.of(chatGptFormatRepliesTools.retrieveFunctionTool()));
        switch (config.getCodeContextPolicy()) {
            case ON_DEMAND:
                ChatGptTools chatGptGetContextTools = new ChatGptTools(ChatGptTools.Functions.getContext);
                tools.add(chatGptGetContextTools.retrieveFunctionTool());
                break;
            case UPLOAD_ALL:
                tools.add(new ChatGptTool("file_search"));
                toolResources = new ChatGptToolResources(
                        new ChatGptToolResources.VectorStoreIds(
                                new String[]{vectorStoreId}
                        )
                );
                break;
        }
    }
}
