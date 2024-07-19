package com.googlesource.gerrit.plugins.chatgpt.logging;

import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public class LoggerFilterDecider {
    private List<String[]> filters;

    public LoggerFilterDecider(String filter) {
        if (!filter.isEmpty()) {
            filters = splitString(unwrapDeSlashQuotes(filter))
                    .stream()
                    .map(s -> {
                        String[] parts = s.split("#");
                        return new String[]{parts[0], parts.length > 1 ? unwrapQuotes(parts[1]) : ""};
                    })
                    .collect(Collectors.toList());
        }
    }

    public boolean shouldOverrideLogLevel(String loggedClassName, String message) {
        return filters != null && filters
                .stream()
                .anyMatch(r -> loggedClassName.contains(r[0]) && message.contains(r[1]));
    }
}
