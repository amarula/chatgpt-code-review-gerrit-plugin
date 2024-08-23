package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.utils.RegexUtils.joinAlternation;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.CODE_DELIMITER;

public class DebugCodeBlocksCleaner {
    private final Pattern debugMessagePattern;

    public DebugCodeBlocksCleaner(Localizer localizer) {
        // Gather all possible message dump titles and combine them into a single Regex string
        String openingTitlesRegex = joinAlternation(
                localizer.filterProperties("message.dump.", ".title")
        );
        debugMessagePattern = Pattern.compile("\\s+" + CODE_DELIMITER +"\\s*(" + openingTitlesRegex + ").*" +
                CODE_DELIMITER + "\\s*", Pattern.DOTALL);
    }

    public String removeDebugCodeBlocks(String message) {
        Matcher debugMessagematcher = debugMessagePattern.matcher(message);
        return debugMessagematcher.replaceAll("");
    }
}
