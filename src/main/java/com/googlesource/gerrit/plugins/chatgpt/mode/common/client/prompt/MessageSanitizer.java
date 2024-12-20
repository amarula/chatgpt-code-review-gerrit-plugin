package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public class MessageSanitizer {
    private static final Pattern SANITIZE_BOLD_REGEX = Pattern.compile("(\\*{1,2}|(?<!\\w)_{1,2})(.+?)\\1",
            Pattern.DOTALL);
    private static final Pattern SANITIZE_NUM_REGEX = Pattern.compile("^(\\s*)(#+)(?=\\s)", Pattern.MULTILINE);

    public static String sanitizeChatGptMessage(String message) {
        log.debug("Sanitizing ChatGPT message.");
        // Sanitize code blocks (delimited by CODE_DELIMITER) by stripping out the language for syntax highlighting and
        // ensuring that is preceded by two "\n" chars. Additionally, sanitize the content outside these blocks.
        String sanitizedMessage = parseOutOfDelimiters(message, "\\s*" + CODE_DELIMITER + "\\w*\\s*",
                MessageSanitizer::sanitizeOutsideInlineCodeBlocks, CODE_DELIMITER_BEGIN, CODE_DELIMITER_END);
        log.debug("Sanitized ChatGPT message: {}", sanitizedMessage);
        return sanitizedMessage;
    }

    private static String sanitizeOutsideInlineCodeBlocks(String message) {
        log.debug("Sanitizing outside inline code blocks.");
        // Sanitize the content outside the inline code blocks (delimited by INLINE_CODE_DELIMITER).
        String sanitizedMessage = parseOutOfDelimiters(message, INLINE_CODE_DELIMITER, MessageSanitizer::sanitizeGerritComment);
        log.debug("Sanitized message outside inline code blocks: {}", sanitizedMessage);
        return sanitizedMessage;
    }

    private static String sanitizeGerritComment(String message) {
        log.debug("Sanitizing Gerrit comment.");
        // Sanitize sequences of asterisks ("*") and underscores ("_") that would be incorrectly interpreted as
        // delimiters of Italic and Bold text.
        Matcher boldSanitizeMatcher = SANITIZE_BOLD_REGEX.matcher(message);
        StringBuilder result = new StringBuilder();
        while (boldSanitizeMatcher.find()) {
            String slashedDelimiter = backslashEachChar(boldSanitizeMatcher.group(1));
            boldSanitizeMatcher.appendReplacement(result, slashedDelimiter + boldSanitizeMatcher.group(2) +
                    slashedDelimiter);
        }
        boldSanitizeMatcher.appendTail(result);
        message = result.toString();
        log.debug("Sanitized bold and italic sequences: {}", message);

        // Sanitize sequences of number signs ("#") that would be incorrectly interpreted as header prefixes.
        Matcher numSanitizeMatcher = SANITIZE_NUM_REGEX.matcher(message);
        message = numSanitizeMatcher.replaceAll("$1\\\\$2");
        log.debug("Sanitized header sequences: {}", message);

        return message;
    }
}
