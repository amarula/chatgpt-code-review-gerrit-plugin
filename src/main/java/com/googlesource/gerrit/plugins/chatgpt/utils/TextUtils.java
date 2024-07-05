package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TextUtils extends StringUtils {
    public static final String INLINE_CODE_DELIMITER = "`";
    public static final String CODE_DELIMITER = "```";
    public static final String CODE_DELIMITER_BEGIN ="\n\n" + CODE_DELIMITER + "\n";
    public static final String CODE_DELIMITER_END ="\n" + CODE_DELIMITER + "\n";
    public static final String SPACE = " ";
    public static final String DOT = ".";
    public static final String COMMA_SPACE = ", ";
    public static final String COLON_SPACE = ": ";
    public static final String SEMICOLON_SPACE = "; ";
    public static final String ITEM_COMMA_DELIMITED = "\\s*,\\s*";
    public static final String QUOTED_ITEM_COMMA_DELIMITED = "(?<!\\\\)\"" + ITEM_COMMA_DELIMITED + "\"";

    public static String parseOutOfDelimiters(String body, String splitDelim, Function<String, String> processMessage,
                                              String leftDelimReplacement, String rightDelimReplacement) {
        String[] chunks = body.split(splitDelim, -1);
        List<String> resultChunks = new ArrayList<>();
        int lastChunk = chunks.length -1;
        for (int i = 0; i <= lastChunk; i++) {
            String chunk = chunks[i];
            if (i % 2 == 0 || i == lastChunk) {
                resultChunks.add(processMessage.apply(chunk));
            }
            else {
                resultChunks.addAll(Arrays.asList(leftDelimReplacement, chunk, rightDelimReplacement));
            }
        }
        return concatenate(resultChunks);
    }

    public static String parseOutOfDelimiters(String body, String splitDelim, Function<String, String> processMessage) {
        return parseOutOfDelimiters(body, splitDelim, processMessage, splitDelim, splitDelim);
    }

    public static String joinWithNewLine(List<String> components) {
        return String.join("\n", components);
    }

    public static String joinWithDoubleNewLine(List<String> components) {
        return String.join("\n\n", components);
    }

    public static String joinWithSpace(List<String> components) {
        return String.join(SPACE, components);
    }

    public static String joinWithComma(Set<String> components) {
        return String.join(COMMA_SPACE, components);
    }

    public static String joinWithSemicolon(List<String> components) {
        return String.join(SEMICOLON_SPACE, components);
    }

    public static List<String> getNumberedList(List<String> components, String prefix, String postfix) {
        return IntStream.range(0, components.size())
                .mapToObj(i -> Optional.ofNullable(prefix).orElse("") +
                        (i + 1) +
                        Optional.ofNullable(postfix).orElse(". ") +
                        components.get(i)
                )
                .collect(Collectors.toList());
    }

    public static String getNumberedListString(List<String> components, String prefix, String postfix) {
        return joinWithSemicolon(getNumberedList(components, prefix, postfix));
    }

    public static String prettyStringifyObject(Object object) {
        List<String> lines = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                lines.add(field.getName() + ": " + field.get(object));
            } catch (IllegalAccessException e) {
                log.debug("Error while accessing field {} in {}", field.getName(), object, e);
            }
        }
        return joinWithNewLine(lines);
    }

    public static String prettyStringifyMap(Map<String, String> map) {
        return joinWithNewLine(
                map.entrySet().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.toList())
        );
    }

    public static List<String> splitString(String value) {
        return splitString(value, ITEM_COMMA_DELIMITED);
    }

    public static List<String> splitString(String value, String delimiter) {
        Pattern separator = Pattern.compile(delimiter);
        return Arrays.asList(separator.split(value));
    }
}
