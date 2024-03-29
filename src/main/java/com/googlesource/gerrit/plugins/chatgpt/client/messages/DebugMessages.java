package com.googlesource.gerrit.plugins.chatgpt.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptReplyItem;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public class DebugMessages {
    private static final String OPENING_TITLE = "DEBUGGING DETAILS";
    private static final String COMMENT_OPENING = CODE_DELIMITER_BEGIN + OPENING_TITLE + "\n";
    private static final String HIDDEN_REPLY = "hidden: %s";
    private static final Pattern DEBUG_MESSAGE_PATTERN = Pattern.compile("\\s+" + CODE_DELIMITER +"\\s*" +
                    OPENING_TITLE + ".*" + CODE_DELIMITER + "\\s*", Pattern.DOTALL);

    public static String getDebugMessage(ChatGptReplyItem replyItem, boolean isHidden) {
        return joinWithNewLine(new ArrayList<>() {{
            add(COMMENT_OPENING);
            add(String.format(HIDDEN_REPLY, isHidden));
            add(prettyStringifyObject(replyItem));
            add(CODE_DELIMITER);
        }});
    }

    public static String removeDebugMessages(String message) {
        Matcher debugMessagematcher = DEBUG_MESSAGE_PATTERN.matcher(message);
        return debugMessagematcher.replaceAll("");
    }

}
