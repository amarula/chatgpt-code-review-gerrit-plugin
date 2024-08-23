package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.prettyStringifyMap;

public class DebugCodeBlocksDynamicConfiguration extends DebugCodeBlocksComposer {
    public DebugCodeBlocksDynamicConfiguration(Localizer localizer) {
        super(localizer, "message.dump.dynamic.configuration.title");
    }

    public String getDebugCodeBlock(Map<String, String> dynamicConfig) {
        return super.getDebugCodeBlock(List.of(
                prettyStringifyMap(dynamicConfig)
        ));
    }
}
