package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.config.dynamic.DynamicConfigManager;
import com.googlesource.gerrit.plugins.chatgpt.config.dynamic.DynamicConfigManagerDirectives;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.DynamicDirectivesModifyException;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDataDump;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDirectives;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistantHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptVectorStoreHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptThread.KEY_THREAD_ID;

@Slf4j
public class ClientCommandExecutor extends ClientCommandBase {
    private static final Set<CommandSet> DYNAMIC_CONFIG_MESSAGE_COMMANDS = Set.of(
            CommandSet.REVIEW,
            CommandSet.REVIEW_LAST,
            CommandSet.CONFIGURE,
            CommandSet.DUMP_CONFIG
    );

    private final ChangeSetData changeSetData;
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;
    private final Localizer localizer;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;

    private CommandSet command;
    private Map<BaseOptionSet, String> baseOptions;
    private Map<String, String> dynamicOptions;
    private String nextString;

    public ClientCommandExecutor(
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        log.debug("ClientCommandExecutor initialized.");
    }

    public void executeCommand(
            CommandSet command,
            Map<BaseOptionSet, String> baseOptions,
            Map<String, String> dynamicOptions,
            String nextString
    ) {
        log.debug("Executing Command: {}, Base Options: {}, Dynamic Options: {}", command, baseOptions, dynamicOptions);
        this.command = command;
        this.baseOptions = baseOptions;
        this.dynamicOptions = dynamicOptions;
        this.nextString = nextString.trim();
        switch (command) {
            case REVIEW, REVIEW_LAST -> commandForceReview(command);
            case UPLOAD_CODEBASE -> commandUploadCodebase();
            case FORGET_THREAD -> commandForgetThread();
            case CONFIGURE -> commandDynamicallyConfigure();
            case DIRECTIVES -> commandDirectives();
            case DUMP_CONFIG -> commandDumpConfig();
            case DUMP_STORED_DATA -> commandDumpStoredData();
        }
    }

    public void postExecuteCommand() {
        if (!DYNAMIC_CONFIG_MESSAGE_COMMANDS.contains(command)) {
            changeSetData.setHideDynamicConfigMessage(true);
        }
    }

    private void commandForceReview(CommandSet command) {
        changeSetData.setForcedReview(true);
        changeSetData.setHideChatGptReview(false);
        changeSetData.setReviewSystemMessage(null);
        if (command == CommandSet.REVIEW_LAST) {
            log.info("Forced review command applied to the last Patch Set");
            changeSetData.setForcedReviewLastPatchSet(true);
        }
        else {
            log.info("Forced review command applied to the entire Change Set");
        }
        if (baseOptions.containsKey(BaseOptionSet.FILTER)) {
            boolean value = Boolean.parseBoolean(baseOptions.get(BaseOptionSet.FILTER));
            log.debug("Option 'replyFilterEnabled' set to {}", value);
            changeSetData.setReplyFilterEnabled(value);
        }
        else if (baseOptions.containsKey(BaseOptionSet.DEBUG)) {
            log.debug("Response Mode set to Debug");
            changeSetData.setDebugReviewMode(true);
            changeSetData.setReplyFilterEnabled(false);
        }
    }

    private void commandUploadCodebase() {
        log.info("Uploading codebase for the project");
        ChatGptAssistantHandler chatGptAssistantHandler = new ChatGptAssistantHandler(
                config,
                changeSetData,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider
        );
        chatGptAssistantHandler.flushAssistantAndVectorIds();

        ChatGptVectorStoreHandler chatGptVectorStoreHandler = new ChatGptVectorStoreHandler(
                config,
                change,
                gitRepoFiles,
                pluginDataHandlerProvider.getProjectScope()
        );
        try {
            chatGptVectorStoreHandler.generateVectorStore();
        }
        catch (Exception OpenAiConnectionFailException) {
            changeSetData.setReviewSystemMessage(localizer.getText("message.command.codebase.upload.error"));
            return;
        }
        changeSetData.setReviewSystemMessage(localizer.getText("message.command.codebase.upload.successful"));
    }

    private void commandForgetThread() {
        PluginDataHandler changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        log.info("Removing thread ID '{}' for Change Set", changeDataHandler.getValue(KEY_THREAD_ID));
        changeDataHandler.removeValue(KEY_THREAD_ID);
        changeSetData.setReviewSystemMessage(localizer.getText("message.command.thread.forget"));
    }

    private void commandDynamicallyConfigure() {
        boolean modifiedDynamicConfig = false;
        boolean shouldResetDynamicConfig = false;
        DynamicConfigManager dynamicConfigManager = new DynamicConfigManager(pluginDataHandlerProvider);

        if (baseOptions.containsKey(BaseOptionSet.RESET)) {
            shouldResetDynamicConfig = true;
            log.debug("Resetting configuration settings");
        }
        if (!dynamicOptions.isEmpty()) {
            modifiedDynamicConfig = true;
            for (Map.Entry<String, String> dynamicOption : dynamicOptions.entrySet()) {
                String optionKey = dynamicOption.getKey();
                String optionValue = dynamicOption.getValue();
                log.debug("Updating configuration setting '{}' to '{}'", optionKey, optionValue);
                dynamicConfigManager.setConfig(optionKey, optionValue);
            }
        }
        dynamicConfigManager.updateConfiguration(modifiedDynamicConfig, shouldResetDynamicConfig);
        changeSetData.setReviewSystemMessage(localizer.getText("message.dump.dynamic.configuration.notify"));
    }

    private void commandDirectives() {
        DynamicConfigManagerDirectives dynamicConfigManagerDirectives = new DynamicConfigManagerDirectives(
                pluginDataHandlerProvider
        );
        DebugCodeBlocksDirectives debugCodeBlocksDirectives = new DebugCodeBlocksDirectives(localizer);
        try {
            if (baseOptions.containsKey(BaseOptionSet.RESET)) {
                dynamicConfigManagerDirectives.resetDirectives();
            } else if (baseOptions.containsKey(BaseOptionSet.REMOVE)) {
                dynamicConfigManagerDirectives.removeDirective(nextString);
            } else if (!nextString.isEmpty()) {
                dynamicConfigManagerDirectives.addDirective(nextString);
            }
        }
        catch (DynamicDirectivesModifyException e) {
            changeSetData.setReviewSystemMessage(localizer.getText("message.dump.directives.modify.error"));
            return;
        }
        changeSetData.setReviewSystemMessage(debugCodeBlocksDirectives.getDebugCodeBlock(
                dynamicConfigManagerDirectives.getDirectives()
        ));
    }

    private void commandDumpConfig() {
        DebugCodeBlocksConfiguration debugCodeBlocksConfiguration = new DebugCodeBlocksConfiguration(localizer);
        changeSetData.setReviewSystemMessage(debugCodeBlocksConfiguration.getDebugCodeBlock(config));
    }

    private void commandDumpStoredData() {
        DebugCodeBlocksDataDump debugCodeBlocksDataDump = new DebugCodeBlocksDataDump(
                localizer,
                pluginDataHandlerProvider
        );
        changeSetData.setReviewSystemMessage(debugCodeBlocksDataDump.getDebugCodeBlock());
    }
}
