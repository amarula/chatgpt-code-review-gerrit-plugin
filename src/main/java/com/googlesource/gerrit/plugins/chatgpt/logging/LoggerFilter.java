package com.googlesource.gerrit.plugins.chatgpt.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class LoggerFilter extends Filter {
    private final List<String[]> filters;
    private final Level originalLevel;

    public LoggerFilter(String filter, Level originalLevel) {
        filters = splitString(unwrapDeSlashQuotes(filter))
                .stream()
                .map(s -> {
                    String[] parts = s.split("#");
                    return new String[]{parts[0], parts.length > 1 ? unwrapQuotes(parts[1]) : ""};
                })
                .collect(Collectors.toList());
        this.originalLevel = originalLevel;

        log.debug("LoggerFilters Level: {}", originalLevel);
    }

    @Override
    public int decide(LoggingEvent event) {
        String loggedClassName = event.getLoggerName();
        String message = event.getMessage().toString();
        Level level = event.getLevel();

        if (level.isGreaterOrEqual(originalLevel) || shouldOverrideLogLevel(loggedClassName, message)) {
            return Filter.ACCEPT;
        }
        else {
            return Filter.DENY;
        }
    }

    private boolean shouldOverrideLogLevel(String loggedClassName, String message) {
        return filters
                .stream()
                .anyMatch(r -> loggedClassName.contains(r[0]) && message.contains(r[1]));
    }
}
