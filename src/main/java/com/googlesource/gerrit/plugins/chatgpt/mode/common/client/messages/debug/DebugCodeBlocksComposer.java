package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public abstract class DebugCodeBlocksComposer {
    protected final Localizer localizer;
    protected final String commentOpening;

    public DebugCodeBlocksComposer(Localizer localizer, String openingTitleKey) {
        this.localizer = localizer;
        String openingTitle = localizer.getText(openingTitleKey);
        commentOpening = CODE_DELIMITER_BEGIN + openingTitle + "\n";
    }

    protected String getDebugCodeBlock(List<String> panelItems) {
        return joinWithNewLine(new ArrayList<>() {{
            add(commentOpening);
            addAll(panelItems);
            add(CODE_DELIMITER);
        }});
    }
}
