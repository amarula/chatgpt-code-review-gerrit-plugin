package com.googlesource.gerrit.plugins.chatgpt.logging;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerBaseProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.*;

@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class LoggingConfiguration {
    private static final String ORIGINAL_LOG_LEVEL = "originalLogLevel";

    public static void configure(Configuration config, PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider) {
        Logger logger = Logger.getRootLogger();
        Appender appender = logger.getAppender("error_log");
        PluginDataHandler globalDataHandler = pluginDataHandlerBaseProvider.get();
        String originalLogLevelStr = globalDataHandler.getValue(ORIGINAL_LOG_LEVEL);
        String selectiveLogLevelOverride = config.getSelectiveLogLevelOverride();
        log.debug("Logger configured for selective override: {} - Appender: {}", logger, appender);
        if (selectiveLogLevelOverride.isEmpty()) {
            if (originalLogLevelStr != null) {
                logger.setLevel(Level.toLevel(originalLogLevelStr));
                log.info("Log Level restored to {}", originalLogLevelStr);
            }
            return;
        }

        Level currentLevel = logger.getLevel();
        Level originalLogLevel;
        if (originalLogLevelStr == null) {
            if (currentLevel.isGreaterOrEqual(Level.INFO)) {
                globalDataHandler.setValue(ORIGINAL_LOG_LEVEL, currentLevel.toString());
                log.info("Recorded current level: {}", currentLevel);
                originalLogLevel = currentLevel;
            }
            else {
                originalLogLevel = Level.INFO;
            }
        }
        else {
            originalLogLevel = Level.toLevel(originalLogLevelStr);
        }
        appender.clearFilters();
        LoggerFilter filter = new LoggerFilter(selectiveLogLevelOverride, originalLogLevel);
        appender.addFilter(filter);
        log.debug("Log Filters added ({}). Current Level: {}", selectiveLogLevelOverride, currentLevel);
        logger.setLevel(Level.DEBUG);
    }
}
