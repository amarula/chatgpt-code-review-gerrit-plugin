package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.DebugCodeBlocksDataDump;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.DebugCodeBlocksConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptThread.KEY_THREAD_ID;

@Slf4j
public class ClientCommandExecutor extends ClientCommandBase {
    private final ChangeSetData changeSetData;
    private final Localizer localizer;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final DynamicConfiguration dynamicConfiguration;

    private Map<BaseOptionSet, String> baseOptions;
    private Map<String, String> dynamicOptions;

    public ClientCommandExecutor(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        dynamicConfiguration = new DynamicConfiguration(pluginDataHandlerProvider);
        log.debug("ClientCommandExecutor initialized.");
    }

    public void executeCommand(
            CommandSet command,
            Map<BaseOptionSet, String> baseOptions,
            Map<String, String> dynamicOptions
    ) {
        log.debug("Executing Command: {}, Base Options: {}, Dynamic Options: {}", command, baseOptions, dynamicOptions);
        this.baseOptions = baseOptions;
        this.dynamicOptions = dynamicOptions;
        switch (command) {
            case REVIEW, REVIEW_LAST -> commandForceReview(command);
            case FORGET_THREAD -> commandForgetThread();
            case CONFIGURE -> commandDynamicallyConfigure();
            case DUMP_CONFIG -> commandDumpConfig();
            case DUMP_STORED_DATA -> commandDumpStoredData();
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

    private void commandForgetThread() {
        PluginDataHandler changeDataHandler = pluginDataHandlerProvider.getChangeScope();
        log.info("Removing thread ID '{}' for Change Set", changeDataHandler.getValue(KEY_THREAD_ID));
        changeDataHandler.removeValue(KEY_THREAD_ID);
        changeSetData.setReviewSystemMessage(localizer.getText("message.command.thread.forget"));
    }

    private void commandDynamicallyConfigure() {
        boolean modifiedDynamicConfig = false;
        boolean shouldResetDynamicConfig = false;
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
                dynamicConfiguration.setConfig(optionKey, optionValue);
            }
        }
        dynamicConfiguration.updateConfiguration(modifiedDynamicConfig, shouldResetDynamicConfig);
    }

    private void commandDumpConfig() {
        DebugCodeBlocksConfiguration debugCodeBlocksConfiguration = new DebugCodeBlocksConfiguration(
                localizer
        );
        changeSetData.setReviewSystemMessage(debugCodeBlocksConfiguration.getDataDumpBlock(config));
    }

    private void commandDumpStoredData() {
        DebugCodeBlocksDataDump debugCodeBlocksDataDump = new DebugCodeBlocksDataDump(
                localizer,
                pluginDataHandlerProvider
        );
        changeSetData.setReviewSystemMessage(debugCodeBlocksDataDump.getDataDumpBlock());
    }
}
