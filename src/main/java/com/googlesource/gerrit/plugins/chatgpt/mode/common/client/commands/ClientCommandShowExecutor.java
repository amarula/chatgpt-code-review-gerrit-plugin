package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksConfiguration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug.DebugCodeBlocksDataDump;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithDoubleNewLine;

@Slf4j
public class ClientCommandShowExecutor extends ClientCommandBase {
    private final ChangeSetData changeSetData;
    private final Localizer localizer;
    private final PluginDataHandlerProvider pluginDataHandlerProvider;
    private final List<String> itemsToShow = new ArrayList<>();

    public ClientCommandShowExecutor(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        this.localizer = localizer;
        this.changeSetData = changeSetData;
        this.pluginDataHandlerProvider = pluginDataHandlerProvider;
        log.debug("ClientShowCommandExecutor initialized.");
    }

    public void executeShowCommand(Map<BaseOptionSet, String> baseOptions) {
        log.debug("Executing Show Command: {}", baseOptions);
        for (BaseOptionSet baseOption : baseOptions.keySet()) {
            switch (baseOption) {
                case CONFIG -> commandDumpConfig();
                case LOCAL_DATA -> commandDumpStoredData();
            }
        }
        changeSetData.setReviewSystemMessage(joinWithDoubleNewLine(itemsToShow));
    }

    private void commandDumpConfig() {
        DebugCodeBlocksConfiguration debugCodeBlocksConfiguration = new DebugCodeBlocksConfiguration(localizer);
        itemsToShow.add(debugCodeBlocksConfiguration.getDebugCodeBlock(config));
    }

    private void commandDumpStoredData() {
        DebugCodeBlocksDataDump debugCodeBlocksDataDump = new DebugCodeBlocksDataDump(
                localizer,
                pluginDataHandlerProvider
        );
        itemsToShow.add(debugCodeBlocksDataDump.getDebugCodeBlock());
    }
}
