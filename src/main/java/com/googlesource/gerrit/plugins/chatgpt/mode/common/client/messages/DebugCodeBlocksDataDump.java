package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.convertPascalCaseToWords;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.prettyStringifyMap;

@Slf4j
public class DebugCodeBlocksDataDump extends DebugCodeBlocksBase {
    private final List<String> dataDump = new ArrayList<>();

    public DebugCodeBlocksDataDump(Localizer localizer, PluginDataHandlerProvider pluginDataHandlerProvider) {
        super(localizer.getText("message.dump.stored.data.title"));
        retrieveStoredData(pluginDataHandlerProvider);
    }

    public String getDataDumpBlock() {
        return super.getDebugCodeBlock(dataDump);
    }

    private void retrieveStoredData(PluginDataHandlerProvider pluginDataHandlerProvider) {
        for (Method method : pluginDataHandlerProvider.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            try {
                String methodName = method.getName();
                log.debug("Retrieving stored method {}", methodName);
                if (!methodName.startsWith("get") || !methodName.endsWith("Scope")) continue;
                String dataKey = methodName.replaceAll("^get", "");
                log.debug("Populating data key {}", dataKey);
                dataDump.add("### " + convertPascalCaseToWords(dataKey));
                PluginDataHandler dataHandler = (PluginDataHandler) method.invoke(pluginDataHandlerProvider);
                try {
                    dataDump.add(prettyStringifyMap(dataHandler.getAllValues()) + "\n");
                }
                catch (Exception e) {
                    log.warn("Exception while retrieving data", e);
                }
            }
            catch (Exception e) {
                log.error("Error while invoking method: {}", method.getName(), e);
                throw new RuntimeException("Error while retrieving stored data", e);
            }
        }
    }
}
