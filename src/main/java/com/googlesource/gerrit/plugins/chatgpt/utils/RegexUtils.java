package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegexUtils {
    private static final String ALTERNATION_OPERATOR = "|";

    public static String joinAlternation(List<String> operands) {
        return String.join(ALTERNATION_OPERATOR, operands);
    }

    public static String patternJoinAlternation(Pattern... operands) {
        return joinAlternation(Stream.of(operands)
                .map(Pattern::pattern)
                .collect(Collectors.toList()));
    }
}
