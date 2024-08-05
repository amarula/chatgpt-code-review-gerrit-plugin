package com.googlesource.gerrit.plugins.chatgpt.config;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;

import com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.Modes;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;
import static java.util.stream.Collectors.toList;

@Slf4j
public class Configuration {
    // Config Constants
    public static final String ENABLED_USERS_ALL = "ALL";
    public static final String ENABLED_GROUPS_ALL = "ALL";
    public static final String ENABLED_TOPICS_ALL = "ALL";
    public static final String NOT_CONFIGURED_ERROR_MSG = "%s is not configured";
    // The convention `KEY_<CONFIG_KEY> = "configKey"` is used for naming config key constants
    public static final String PREFIX_KEY = "KEY_";
    // The convention is "getConfigKey"` used for naming config key getter methods
    public static final String PREFIX_GETTER = "get";

    // Default Config values
    public static final String OPENAI_DOMAIN = "https://api.openai.com";
    public static final String DEFAULT_GPT_MODEL = "gpt-4o";
    public static final double DEFAULT_GPT_REVIEW_TEMPERATURE = 0.2;
    public static final double DEFAULT_GPT_COMMENT_TEMPERATURE = 1.0;

    private static final String DEFAULT_GPT_MODE = "stateless";
    private static final boolean DEFAULT_REVIEW_PATCH_SET = true;
    private static final boolean DEFAULT_REVIEW_COMMIT_MESSAGES = true;
    private static final boolean DEFAULT_FULL_FILE_REVIEW = true;
    private static final boolean DEFAULT_STREAM_OUTPUT = false;
    private static final boolean DEFAULT_GLOBAL_ENABLE = false;
    private static final String DEFAULT_DISABLED_USERS = "";
    private static final String DEFAULT_ENABLED_USERS = ENABLED_USERS_ALL;
    private static final String DEFAULT_DISABLED_GROUPS = "";
    private static final String DEFAULT_ENABLED_GROUPS = ENABLED_GROUPS_ALL;
    private static final String DEFAULT_DISABLED_TOPIC_FILTER = "";
    private static final String DEFAULT_ENABLED_TOPIC_FILTER = ENABLED_TOPICS_ALL;
    private static final String DEFAULT_ENABLED_PROJECTS = "";
    private static final String DEFAULT_ENABLED_FILE_EXTENSIONS = String.join(",", new String[]{
            ".py",
            ".java",
            ".js",
            ".ts",
            ".html",
            ".css",
            ".cs",
            ".cpp",
            ".c",
            ".h",
            ".php",
            ".rb",
            ".swift",
            ".kt",
            ".r",
            ".jl",
            ".go",
            ".scala",
            ".pl",
            ".pm",
            ".rs",
            ".dart",
            ".lua",
            ".sh",
            ".vb",
            ".bat"
    });
    private static final boolean DEFAULT_PROJECT_ENABLE = false;
    private static final String DEFAULT_DIRECTIVES = "";
    private static final int DEFAULT_MAX_REVIEW_LINES = 1000;
    private static final int DEFAULT_MAX_REVIEW_FILE_SIZE = 10000;
    private static final boolean DEFAULT_ENABLED_VOTING = false;
    private static final boolean DEFAULT_FILTER_NEGATIVE_COMMENTS = true;
    private static final int DEFAULT_FILTER_COMMENTS_BELOW_SCORE = 0;
    private static final boolean DEFAULT_FILTER_RELEVANT_COMMENTS = true;
    private static final double DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD = 0.6;
    private static final int DEFAULT_VOTING_MIN_SCORE = -1;
    private static final int DEFAULT_VOTING_MAX_SCORE = 1;
    private static final boolean DEFAULT_INLINE_COMMENTS_AS_RESOLVED = false;
    private static final boolean DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED = false;
    private static final boolean DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS = false;
    private static final boolean DEFAULT_IGNORE_RESOLVED_CHAT_GPT_COMMENTS = true;
    private static final boolean DEFAULT_FORCE_CREATE_ASSISTANT = false;
    private static final boolean DEFAULT_TASK_SPECIFIC_ASSISTANTS = false;
    private static final boolean DEFAULT_ENABLE_MESSAGE_DEBUGGING = false;
    private static final String DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE = "";

    // Config setting keys
    public static final String KEY_GPT_SYSTEM_PROMPT = "gptSystemPrompt";
    public static final String KEY_GPT_RELEVANCE_RULES = "gptRelevanceRules";
    public static final String KEY_GPT_REVIEW_TEMPERATURE = "gptReviewTemperature";
    public static final String KEY_GPT_COMMENT_TEMPERATURE = "gptCommentTemperature";
    public static final String KEY_VOTING_MIN_SCORE = "votingMinScore";
    public static final String KEY_VOTING_MAX_SCORE = "votingMaxScore";
    public static final String KEY_GERRIT_USERNAME = "gerritUserName";

    private static final String KEY_GPT_TOKEN = "gptToken";
    private static final String KEY_GPT_DOMAIN = "gptDomain";
    private static final String KEY_GPT_MODEL = "gptModel";
    private static final String KEY_STREAM_OUTPUT = "gptStreamOutput";
    private static final String KEY_GPT_MODE = "gptMode";
    private static final String KEY_REVIEW_COMMIT_MESSAGES = "gptReviewCommitMessages";
    private static final String KEY_REVIEW_PATCH_SET = "gptReviewPatchSet";
    private static final String KEY_FULL_FILE_REVIEW = "gptFullFileReview";
    private static final String KEY_PROJECT_ENABLE = "isEnabled";
    private static final String KEY_GLOBAL_ENABLE = "globalEnable";
    private static final String KEY_DIRECTIVES = "directives";
    private static final String KEY_DISABLED_USERS = "disabledUsers";
    private static final String KEY_ENABLED_USERS = "enabledUsers";
    private static final String KEY_DISABLED_GROUPS = "disabledGroups";
    private static final String KEY_ENABLED_GROUPS = "enabledGroups";
    private static final String KEY_DISABLED_TOPIC_FILTER = "disabledTopicFilter";
    private static final String KEY_ENABLED_TOPIC_FILTER = "enabledTopicFilter";
    private static final String KEY_ENABLED_PROJECTS = "enabledProjects";
    private static final String KEY_MAX_REVIEW_LINES = "maxReviewLines";
    private static final String KEY_MAX_REVIEW_FILE_SIZE = "maxReviewFileSize";
    private static final String KEY_ENABLED_FILE_EXTENSIONS = "enabledFileExtensions";
    private static final String KEY_ENABLED_VOTING = "enabledVoting";
    private static final String KEY_FILTER_NEGATIVE_COMMENTS = "filterNegativeComments";
    private static final String KEY_FILTER_COMMENTS_BELOW_SCORE = "filterCommentsBelowScore";
    private static final String KEY_FILTER_RELEVANT_COMMENTS = "filterRelevantComments";
    private static final String KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD = "filterCommentsRelevanceThreshold";
    private static final String KEY_INLINE_COMMENTS_AS_RESOLVED = "inlineCommentsAsResolved";
    private static final String KEY_PATCH_SET_COMMENTS_AS_RESOLVED = "patchSetCommentsAsResolved";
    private static final String KEY_IGNORE_OUTDATED_INLINE_COMMENTS = "ignoreOutdatedInlineComments";
    private static final String KEY_IGNORE_RESOLVED_CHAT_GPT_COMMENTS = "ignoreResolvedChatGptComments";
    private static final String KEY_FORCE_CREATE_ASSISTANT = "forceCreateAssistant";
    private static final String KEY_TASK_SPECIFIC_ASSISTANTS = "taskSpecificAssistants";
    private static final String KEY_ENABLE_MESSAGE_DEBUGGING = "enableMessageDebugging";
    private static final String KEY_SELECTIVE_LOG_LEVEL_OVERRIDE = "selectiveLogLevelOverride";

    private static final List<String> EXCLUDE_FROM_DUMP = List.of("KEY_GPT_TOKEN");

    private final OneOffRequestContext context;
    @Getter
    private final Account.Id userId;
    @Getter
    private final PluginConfig globalConfig;
    @Getter
    private final PluginConfig projectConfig;
    @Getter
    private final String gerritUserEmail;
    @Getter
    private final GerritApi gerritApi;


    public Configuration(OneOffRequestContext context, GerritApi gerritApi, PluginConfig globalConfig, PluginConfig projectConfig, String gerritUserEmail, Account.Id userId) {
        this.context = context;
        this.gerritApi = gerritApi;
        this.globalConfig = globalConfig;
        this.projectConfig = projectConfig;
        this.gerritUserEmail = gerritUserEmail;
        this.userId = userId;
    }

    public ManualRequestContext openRequestContext() {
      return context.openAs(userId);
    }

    public String getGptToken() {
        return getValidatedOrThrow(KEY_GPT_TOKEN);
    }

    public String getGerritUserName() {
        return getValidatedOrThrow(KEY_GERRIT_USERNAME);
    }

    public String getGptDomain() {
        return getString(KEY_GPT_DOMAIN, OPENAI_DOMAIN);
    }

    public String getGptModel() {
        return getString(KEY_GPT_MODEL, DEFAULT_GPT_MODEL);
    }

    public boolean getGptReviewPatchSet() {
        return getBoolean(KEY_REVIEW_PATCH_SET, DEFAULT_REVIEW_PATCH_SET);
    }

    public Modes getGptMode() {
        String mode = getString(KEY_GPT_MODE, DEFAULT_GPT_MODE);
        try {
            return Enum.valueOf(Modes.class, mode);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Illegal mode: " + mode, e);
        }
    }

    public boolean getGptReviewCommitMessages() {
        return getBoolean(KEY_REVIEW_COMMIT_MESSAGES, DEFAULT_REVIEW_COMMIT_MESSAGES);
    }

    public boolean getGptFullFileReview() {
        return getBoolean(KEY_FULL_FILE_REVIEW, DEFAULT_FULL_FILE_REVIEW);
    }

    public boolean getGptStreamOutput() {
        return getBoolean(KEY_STREAM_OUTPUT, DEFAULT_STREAM_OUTPUT);
    }

    public boolean isProjectEnable() {
        return projectConfig.getBoolean(KEY_PROJECT_ENABLE, DEFAULT_PROJECT_ENABLE);
    }

    public boolean isGlobalEnable() {
        return globalConfig.getBoolean(KEY_GLOBAL_ENABLE, DEFAULT_GLOBAL_ENABLE);
    }

    public List<String> getDisabledUsers() {
        return splitConfig(getString(KEY_DISABLED_USERS, DEFAULT_DISABLED_USERS));
    }

    public List<String> getEnabledUsers() {
        return splitConfig(getString(KEY_ENABLED_USERS, DEFAULT_ENABLED_USERS));
    }

    public List<String> getDisabledGroups() {
        return splitConfig(getString(KEY_DISABLED_GROUPS, DEFAULT_DISABLED_GROUPS));
    }

    public List<String> getEnabledGroups() {
        return splitConfig(getString(KEY_ENABLED_GROUPS, DEFAULT_ENABLED_GROUPS));
    }

    public List<String> getDisabledTopicFilter() {
        return splitConfig(getString(KEY_DISABLED_TOPIC_FILTER, DEFAULT_DISABLED_TOPIC_FILTER));
    }

    public List<String> getEnabledTopicFilter() {
        return splitConfig(getString(KEY_ENABLED_TOPIC_FILTER, DEFAULT_ENABLED_TOPIC_FILTER));
    }

    public String getEnabledProjects() {
        return globalConfig.getString(KEY_ENABLED_PROJECTS, DEFAULT_ENABLED_PROJECTS);
    }

    public int getMaxReviewLines() {
        return getInt(KEY_MAX_REVIEW_LINES, DEFAULT_MAX_REVIEW_LINES);
    }

    public int getMaxReviewFileSize() {
        return getInt(KEY_MAX_REVIEW_FILE_SIZE, DEFAULT_MAX_REVIEW_FILE_SIZE);
    }

    public List<String> getEnabledFileExtensions() {
        return splitConfig(getString(KEY_ENABLED_FILE_EXTENSIONS, DEFAULT_ENABLED_FILE_EXTENSIONS));
    }

    public List<String> getDirectives() {
        return splitConfig(getString(KEY_DIRECTIVES, DEFAULT_DIRECTIVES), TextUtils.QUOTED_ITEM_COMMA_DELIMITED)
                .stream()
                .filter(s -> !s.isEmpty())
                .map(s -> StringUtils.appendIfMissing(StringUtils.strip(s, TextUtils.DOUBLE_QUOTES), "."))
                .collect(toList());
    }

    public boolean isVotingEnabled() {
        return getBoolean(KEY_ENABLED_VOTING, DEFAULT_ENABLED_VOTING);
    }

    public boolean getFilterNegativeComments() {
        return getBoolean(KEY_FILTER_NEGATIVE_COMMENTS, DEFAULT_FILTER_NEGATIVE_COMMENTS);
    }

    public int getFilterCommentsBelowScore() {
        return getInt(KEY_FILTER_COMMENTS_BELOW_SCORE, DEFAULT_FILTER_COMMENTS_BELOW_SCORE);
    }

    public boolean getFilterRelevantComments() {
        return getBoolean(KEY_FILTER_RELEVANT_COMMENTS, DEFAULT_FILTER_RELEVANT_COMMENTS);
    }

    public double getFilterCommentsRelevanceThreshold() {
        return getDouble(KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD, DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD);
    }

    public Locale getLocaleDefault() {
        return Locale.getDefault();
    }

    public int getVotingMinScore() {
        return getInt(KEY_VOTING_MIN_SCORE, DEFAULT_VOTING_MIN_SCORE);
    }

    public int getVotingMaxScore() {
        return getInt(KEY_VOTING_MAX_SCORE, DEFAULT_VOTING_MAX_SCORE);
    }

    public boolean getInlineCommentsAsResolved() {
        return getBoolean(KEY_INLINE_COMMENTS_AS_RESOLVED, DEFAULT_INLINE_COMMENTS_AS_RESOLVED);
    }

    public boolean getPatchSetCommentsAsResolved() {
        return getBoolean(KEY_PATCH_SET_COMMENTS_AS_RESOLVED, DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED);
    }

    public boolean getIgnoreResolvedChatGptComments() {
        return getBoolean(KEY_IGNORE_RESOLVED_CHAT_GPT_COMMENTS, DEFAULT_IGNORE_RESOLVED_CHAT_GPT_COMMENTS);
    }

    public boolean getForceCreateAssistant() {
        return getBoolean(KEY_FORCE_CREATE_ASSISTANT, DEFAULT_FORCE_CREATE_ASSISTANT);
    }

    public boolean getTaskSpecificAssistants() {
        return getBoolean(KEY_TASK_SPECIFIC_ASSISTANTS, DEFAULT_TASK_SPECIFIC_ASSISTANTS);
    }

    public boolean getEnableMessageDebugging() {
        return getBoolean(KEY_ENABLE_MESSAGE_DEBUGGING, DEFAULT_ENABLE_MESSAGE_DEBUGGING);
    }

    public boolean getIgnoreOutdatedInlineComments() {
        return getBoolean(KEY_IGNORE_OUTDATED_INLINE_COMMENTS, DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS);
    }

    public String getSelectiveLogLevelOverride() {
        return getProjectGlobalString(KEY_SELECTIVE_LOG_LEVEL_OVERRIDE, DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE);
    }

    public String getString(String key, String defaultValue) {
        String value = projectConfig.getString(key);
        if (value != null) {
            return value;
        }
        return globalConfig.getString(key, defaultValue);
    }

    public String getProjectGlobalString(String key, String defaultValue) {
        String globalValue = globalConfig.getString(key, defaultValue);
        String projectValue = projectConfig.getString(key, defaultValue);
        log.debug("Get project global string - globalConfig: {}, projectConfig: {}", globalValue, projectValue);

        String resultValue = joinWithComma(
                Stream.of(globalValue, projectValue)
                        .filter(s -> !s.isEmpty())
                        .map(TextUtils::unwrapQuotes)
                        .collect(Collectors.toSet())
        );
        return resultValue.isEmpty() ? "" : wrapQuotes(resultValue);
    }

    public boolean isDefinedKey(String key) {
        try {
            String configKey = PREFIX_KEY + convertCamelToSnakeCase(key).toUpperCase();
            log.debug("Checking if config key `{}` for {} is defined", configKey, key);
            Field field = this.getClass().getDeclaredField(configKey);
            String value = getFieldConfigValue(field);
            log.debug("Config key value: {}", value);
            return value.equals(key);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Error checking if config key `{}` is defined", key, e);
            return false;
        }
    }

    public TreeMap<String, String> dumpConfigMap() {
        log.debug("Start dumping config map");
        TreeMap<String, String> configMap = new TreeMap<>();
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                String fieldName = field.getName();
                log.debug("Processing dumping config field `{}`", fieldName);
                if (!fieldName.startsWith(PREFIX_KEY) || EXCLUDE_FROM_DUMP.contains(fieldName)) {
                    continue;
                }
                String fieldValue = getFieldConfigValue(field);
                String getterName = PREFIX_GETTER + capitalizeFirstLetter(fieldValue);
                String configValue;
                try {
                    configValue = this.getClass().getDeclaredMethod(getterName).invoke(this).toString();
                } catch (NoSuchMethodException e) {
                    log.debug("Config field `{}` lacking getter method `{}` is excluded from the dump", fieldName,
                            getterName);
                    continue;
                }
                log.debug("Config entities retrieved - Field Value: `{}`, Getter Name: `{}`, Config Value: `{}`",
                        fieldValue, getterName, configValue);
                configMap.put(fieldValue, configValue);
            }
            return configMap;
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.info("Error retrieving configuration", e);
            return null;
        }
    }

    private String getFieldConfigValue(Field field) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(null).toString();
    }

    private String getValidatedOrThrow(String key) {
        String value = projectConfig.getString(key);
        if (value == null) {
            value = globalConfig.getString(key);
        }
        if (value == null) {
            throw new RuntimeException(String.format(NOT_CONFIGURED_ERROR_MSG, key));
        }
        return value;
    }

    private int getInt(String key, int defaultValue) {
        int valueForProject = projectConfig.getInt(key, defaultValue);
        // To avoid misinterpreting an undefined value as zero, a secondary check is performed by retrieving the value
        // as a String.
        if (valueForProject != defaultValue && valueForProject != 0
                && projectConfig.getString(key, "") != null) {
            return valueForProject;
        }
        return globalConfig.getInt(key, defaultValue);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        boolean valueForProject = projectConfig.getBoolean(key, defaultValue);
        if (projectConfig.getString(key) != null) {
            return valueForProject;
        }
        return globalConfig.getBoolean(key, defaultValue);
    }

    private Double getDouble(String key, Double defaultValue) {
        return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
    }

    private List<String> splitConfig(String value) {
        return TextUtils.splitString(value);
    }

    private List<String> splitConfig(String value, String delimiter) {
        return TextUtils.splitString(value, delimiter);
    }
}
