package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.prettyStringifyMap;

public class DebugCodeBlocksDynamicConfiguration extends DebugCodeBlocksBase {
    public DebugCodeBlocksDynamicConfiguration(Localizer localizer) {
        super(localizer.getText("message.dynamic.configuration.title"));
    }

    public String getDebugCodeBlock(Map<String, String> dynamicConfig) {
        return super.getDebugCodeBlock(List.of(
                prettyStringifyMap(dynamicConfig)
        ));
    }
}
