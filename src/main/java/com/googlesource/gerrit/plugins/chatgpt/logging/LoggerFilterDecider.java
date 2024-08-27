package com.googlesource.gerrit.plugins.chatgpt.logging;

import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.prettyFormatList;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public class LoggerFilterDecider {
    private String filterString;
    private List<String[]> filters;

    public LoggerFilterDecider(String filter) {
        if (!filter.isEmpty()) {
            filterString = unwrapDeSlashQuotes(filter);
            filters = splitLoggerString(splitString(filterString));
        }
    }

    public LoggerFilterDecider(List<String> filters) {
        if (!filters.isEmpty()) {
            filterString = prettyFormatList(filters);
            this.filters = splitLoggerString(filters);
        }
    }

    public boolean shouldOverrideLogLevel(String loggedClassName, String message) {
        return filters != null && !message.contains(filterString) && filters
                .stream()
                .anyMatch(r -> loggedClassName.contains(r[0]) && message.startsWith(r[1]));
    }

    private List<String[]> splitLoggerString(List<String> filters) {
        return filters.stream()
                .map(s -> {
                    String[] parts = s.split("\\|");
                    return new String[]{parts[0], parts.length > 1 ? unwrapQuotes(parts[1]) : ""};
                })
                .collect(Collectors.toList());
    }
}
