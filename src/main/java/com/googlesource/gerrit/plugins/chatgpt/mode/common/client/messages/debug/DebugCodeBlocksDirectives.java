package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.getNumberedList;

public class DebugCodeBlocksDirectives extends DebugCodeBlocksComposer {
    public DebugCodeBlocksDirectives(Localizer localizer) {
        super(localizer, "message.dump.directives.title");
    }

    public String getDebugCodeBlock(List<String> directives) {
        if (directives == null || directives.isEmpty()) {
            return localizer.getText("message.dump.directives.empty");
        }
        return super.getDebugCodeBlock(getNumberedList(directives));
    }
}
