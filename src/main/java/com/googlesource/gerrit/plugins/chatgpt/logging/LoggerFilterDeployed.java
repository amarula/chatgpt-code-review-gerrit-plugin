package com.googlesource.gerrit.plugins.chatgpt.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class LoggerFilterDeployed extends Filter {
    private final LoggerFilterDecider loggerFilterDecider;
    private final Level thresholdLevel;

    public LoggerFilterDeployed(String filter, Level thresholdLevel) {
        loggerFilterDecider = new LoggerFilterDecider(filter);
        this.thresholdLevel = thresholdLevel;
        log.debug("LoggerFilters Level: {}", thresholdLevel);
    }

    @Override
    public int decide(LoggingEvent event) {
        String loggedClassName = event.getLoggerName();
        String message = event.getMessage().toString();
        Level level = event.getLevel();

        if (level.isGreaterOrEqual(thresholdLevel) || loggerFilterDecider.shouldOverrideLogLevel(loggedClassName, message)) {
            return Filter.ACCEPT;
        }
        else {
            return Filter.DENY;
        }
    }
}
