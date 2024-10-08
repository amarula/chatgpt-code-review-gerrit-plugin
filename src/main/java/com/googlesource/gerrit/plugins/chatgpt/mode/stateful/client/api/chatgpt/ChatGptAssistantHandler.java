package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptAssistant;
import com.googlesource.gerrit.plugins.chatgpt.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TimeUtils.now;

@Slf4j
public class ChatGptAssistantHandler extends ClientBase {
    private static final String ASSISTANT_ID_LOG = "assistantIdLog";

    private final GerritChange change;
    private final ICodeContextPolicy codeContextPolicy;
    private final PluginDataHandler changeDataHandler;
    private final PluginDataHandler assistantsDataHandler;
    private final ChatGptAssistant chatGptAssistant;

    public ChatGptAssistantHandler (
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            ICodeContextPolicy codeContextPolicy,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config);
        this.change = change;
        this.codeContextPolicy = codeContextPolicy;
        changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        assistantsDataHandler = pluginDataHandlerProvider.getAssistantsWorkspace();
        chatGptAssistant = new ChatGptAssistant(config, changeSetData, change, codeContextPolicy);
        log.debug("Initialized ChatGptAssistant with project and assistants data handlers.");
    }

    public String setupAssistant() throws OpenAiConnectionFailException {
        log.debug("Setting up the assistant parameters.");
        String assistantIdHashKey = calculateAssistantIdHashKey();
        log.info("Calculated assistant id hash key: {}", assistantIdHashKey);
        String assistantId = assistantsDataHandler.getValue(assistantIdHashKey);
        if (assistantId == null || config.getForceCreateAssistant()) {
            log.debug("Setup Assistant for project {}", change.getProjectNameKey());
            String vectorStoreId = codeContextPolicy.generateVectorStore();
            assistantId = chatGptAssistant.createAssistant(vectorStoreId);
            assistantsDataHandler.setValue(assistantIdHashKey, assistantId);
            log.info("Project Assistant created with ID: {}", assistantId);
        }
        else {
            log.info("Assistant found for the parameter configuration. Assistant ID: {}", assistantId);
        }
        changeDataHandler.appendJsonValue(ASSISTANT_ID_LOG, now() + ": " + assistantId, String.class);

        return assistantId;
    }

    public void flushAssistantAndVectorIds() {
        log.debug("Flushing Assistant and Vector Store IDs.");
        codeContextPolicy.removeVectorStore();
        assistantsDataHandler.destroy();
    }

    private String calculateAssistantIdHashKey() {
        log.debug("Calculating hash key for assistant ID.");
        return HashUtils.hashData(new ArrayList<>(List.of(
                chatGptAssistant.getDescription(),
                chatGptAssistant.getInstructions(),
                chatGptAssistant.getModel(),
                chatGptAssistant.getTemperature().toString()
        )));
    }
}
