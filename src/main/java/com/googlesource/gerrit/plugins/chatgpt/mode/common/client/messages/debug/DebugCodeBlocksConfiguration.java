package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.List;
import java.util.TreeMap;

import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.prettyStringifyMap;

public class DebugCodeBlocksConfiguration extends DebugCodeBlocksBase {
    private final Localizer localizer;

    public DebugCodeBlocksConfiguration(Localizer localizer) {
        super(localizer.getText("message.dump.configuration.title"));
        this.localizer = localizer;
    }

    public String getDebugCodeBlock(Configuration config) {
        TreeMap<String, String> configMap = config.dumpConfigMap();
        if (configMap == null) {
            return localizer.getText("message.dump.configuration.error");
        }
        return super.getDebugCodeBlock(List.of(
                prettyStringifyMap(configMap)
        ));
    }
}
