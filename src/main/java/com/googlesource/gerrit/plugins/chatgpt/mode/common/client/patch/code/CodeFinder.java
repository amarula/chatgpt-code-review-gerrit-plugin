package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.code;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.patch.code.CodeFinderDiff;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.patch.diff.DiffContent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CodeFinder {
    private static final String PUNCTUATION_REGEX = "([()\\[\\]{}<>:;,?&+\\-*/%|=])";
    private static final String BEGINNING_DIFF_REGEX = "(?:^|\n)[+\\-]";
    private static final String ENDING_ELLIPSIS_REGEX = "\\.\\.\\.\\W*$";

    private final String NON_PRINTING_REPLACEMENT;
    private final String PUNCTUATION_REPLACEMENT;
    private final String PLACEHOLDER_REGEX;
    private final List<CodeFinderDiff> codeFinderDiffs;

    private int commentedLine;
    private Pattern commentedCodePattern;
    private GerritCodeRange currentCodeRange;
    private GerritCodeRange closestCodeRange;

    public CodeFinder(List<CodeFinderDiff> codeFinderDiffs, String randomPlaceholder) {
        this.codeFinderDiffs = codeFinderDiffs;
        NON_PRINTING_REPLACEMENT = "\\\\E" + randomPlaceholder +"\\\\Q";
        PUNCTUATION_REPLACEMENT = "\\\\E" + randomPlaceholder +"\\\\$1" + randomPlaceholder +"\\\\Q";
        PLACEHOLDER_REGEX = "(?:" + randomPlaceholder + ")+";
        log.debug("Initialized CodeFinder with placeholder patterns.");
    }

    public GerritCodeRange findCommentedCode(ChatGptReplyItem replyItem, int commentedLine) {
        this.commentedLine = commentedLine;
        updateCodePattern(replyItem);
        currentCodeRange = null;
        closestCodeRange = null;
        for (CodeFinderDiff codeFinderDiff : codeFinderDiffs) {
            for (Field diffField : DiffContent.class.getDeclaredFields()) {
                String diffCode = getDiffItem(diffField, codeFinderDiff.getContent());
                if (diffCode != null) {
                    TreeMap<Integer, Integer> charToLineMapItem = codeFinderDiff.getCharToLineMap();
                    try {
                        findCodeLines(diffCode, charToLineMapItem);
                    }
                    catch (IllegalArgumentException e) {
                        log.warn("Could not retrieve line number from charToLineMap for diff code: {}", diffCode, e);
                    }
                }
                else {
                    log.debug("Diff code is null for field: {}", diffField.getName());
                }
            }
        }
        log.debug("Returning closest code range found.");
        return closestCodeRange;
    }

    private void updateCodePattern(ChatGptReplyItem replyItem) {
        log.debug("Updating code pattern based on the reply item's code snippet.");
        String commentedCode = replyItem.getCodeSnippet()
                .replaceAll(BEGINNING_DIFF_REGEX, "")
                .replaceAll(ENDING_ELLIPSIS_REGEX, "")
                .trim();
        String commentedCodeRegex = Pattern.quote(commentedCode);
        // Generalize the regex to capture snippets where existing sequences of non-printing chars have been modified
        // from the original code
        commentedCodeRegex = commentedCodeRegex.replaceAll("\\s+", NON_PRINTING_REPLACEMENT);
        // Generalize the regex to capture snippets where non-printing chars have been removed from around the
        // punctuation marks of the original code
        commentedCodeRegex = commentedCodeRegex.replaceAll(PUNCTUATION_REGEX, PUNCTUATION_REPLACEMENT);
        // Remove redundant empty literal escape sequences that could have resulted from previous substitutions
        commentedCodeRegex = commentedCodeRegex.replaceAll("\\\\Q\\\\E", "");
        // Obtain a functional regex to match code snippets without relying on non-printing chars
        commentedCodeRegex = commentedCodeRegex.replaceAll(PLACEHOLDER_REGEX, "\\\\s*");
        // Remove any detected trailing matching sequence of non-printing chars
        commentedCodeRegex = commentedCodeRegex.replaceAll("\\\\s\\*$", "");
        commentedCodePattern = Pattern.compile(commentedCodeRegex);
        log.debug("Updated commented code pattern: {}", commentedCodePattern);
    }

    private double calcCodeDistance(GerritCodeRange range, int fromLine) {
        return Math.abs((range.endLine - range.startLine) / 2 - fromLine);
    }

    private String getDiffItem(Field diffField, DiffContent diffItem) {
        try {
            return (String) diffField.get(diffItem);
        }
        catch (IllegalAccessException e) {
            log.error("Error accessing field '{}' during diff processing", diffField.getName(), e);
            return null;
        }
    }

    private int getLineNumber(TreeMap<Integer, Integer> charToLineMapItem, int position) {
        Integer floorPosition = charToLineMapItem.floorKey(position);
        if (floorPosition == null) {
            throw new IllegalArgumentException("Position: " + position);
        }
        return charToLineMapItem.get(floorPosition);
    }

    private int getLineCharacter(String diffCode, int position) {
        // Return the offset relative to the nearest preceding newline character if found, `position` otherwise
        return position - diffCode.substring(0, position).lastIndexOf("\n") -1;
    }

    private void findCodeLines(String diffCode, TreeMap<Integer, Integer> charToLineMapItem)
            throws IllegalArgumentException {
        Matcher codeMatcher = commentedCodePattern.matcher(diffCode);
        while (codeMatcher.find()) {
            int startPosition = codeMatcher.start();
            int endPosition = codeMatcher.end();
            int startLine = getLineNumber(charToLineMapItem, startPosition);
            int endLine = getLineNumber(charToLineMapItem, endPosition);
            if (startLine > endLine) {
                log.info("Code range discarded: start line ({}) greater than end line ({}).\ncodeMatcher: {}.\n" +
                        "diffCode: {}", startLine, endLine, codeMatcher, diffCode);
                continue;
            }
            int startCharacter = getLineCharacter(diffCode, startPosition);
            int endCharacter = getLineCharacter(diffCode, endPosition);
            if (startLine == endLine && startCharacter > endCharacter) {
                log.info("Code range discarded: start char ({}) greater than end char ({}) for line {}.\ncodeMatcher:" +
                        " {}.\ndiffCode: {}", startCharacter, endCharacter, startLine, codeMatcher, diffCode);
                continue;
            }
            currentCodeRange = GerritCodeRange.builder()
                    .startLine(startLine)
                    .endLine(endLine)
                    .startCharacter(startCharacter)
                    .endCharacter(endCharacter)
                    .build();
            log.debug("Evaluated current code range: {}", currentCodeRange);
            // If multiple commented code portions are found and currentCommentRange is closer to the line
            // number suggested by ChatGPT than closestCommentRange, it becomes the new closestCommentRange
            if (closestCodeRange == null || calcCodeDistance(currentCodeRange, commentedLine) <
                    calcCodeDistance(closestCodeRange, commentedLine)) {
                closestCodeRange = currentCodeRange.toBuilder().build();
                log.debug("New closest code range set: {}", closestCodeRange);
            }
        }
    }
}
