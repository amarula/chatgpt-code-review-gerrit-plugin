package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public abstract class DebugCodeBlocksBase {
    protected final String commentOpening;
    protected final Pattern debugMessagePattern;

    public DebugCodeBlocksBase(String openingTitle) {
        commentOpening = CODE_DELIMITER_BEGIN + openingTitle + "\n";
        debugMessagePattern = Pattern.compile("\\s+" + CODE_DELIMITER +"\\s*" + openingTitle + ".*" +
                CODE_DELIMITER + "\\s*", Pattern.DOTALL);
    }

    public String removeDebugCodeBlocks(String message) {
        Matcher debugMessagematcher = debugMessagePattern.matcher(message);
        return debugMessagematcher.replaceAll("");
    }

    protected String getDebugCodeBlock(List<String> panelItems) {
        return joinWithNewLine(new ArrayList<>() {{
            add(commentOpening);
            addAll(panelItems);
            add(CODE_DELIMITER);
        }});
    }
}
