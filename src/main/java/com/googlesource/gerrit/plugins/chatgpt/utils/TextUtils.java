package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class TextUtils extends StringUtils {
    public static final String INLINE_CODE_DELIMITER = "`";
    public static final String CODE_DELIMITER = "```";
    public static final String CODE_DELIMITER_BEGIN ="\n\n" + CODE_DELIMITER + "\n";
    public static final String CODE_DELIMITER_END ="\n" + CODE_DELIMITER + "\n";

    private static final String COMMA = ", ";
    private static final String SEMICOLON = "; ";

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

    public static String joinWithComma(Set<String> components) {
        return String.join(COMMA, components);
    }

    public static String joinWithSemicolon(List<String> components) {
        return String.join(SEMICOLON, components);
    }

    public static List<String> getNumberedList(List<String> components) {
        return IntStream.range(0, components.size())
                .mapToObj(i -> (i + 1) + ". " + components.get(i))
                .collect(Collectors.toList());
    }

    public static String getNumberedListString(List<String> components) {
        return joinWithSemicolon(getNumberedList(components));
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
}
