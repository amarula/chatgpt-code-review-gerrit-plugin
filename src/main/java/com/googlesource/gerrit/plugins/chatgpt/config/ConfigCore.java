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
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.capitalizeFirstLetter;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.convertCamelToSnakeCase;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithComma;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.wrapQuotes;
import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class ConfigCore {
    private static final Set<String> EXCLUDE_FROM_DUMP = Set.of("KEY_GPT_TOKEN");

    public static final String NOT_CONFIGURED_ERROR_MSG = "%s is not configured";
    // The convention `KEY_<CONFIG_KEY> = "configKey"` is used for naming config key constants
    public static final String PREFIX_KEY = "KEY_";
    // The convention "getConfigKey"` is used for naming config key getter methods
    public static final String PREFIX_GETTER = "get";

    protected final OneOffRequestContext context;
    @Getter
    protected final Account.Id userId;
    @Getter
    protected final PluginConfig globalConfig;
    @Getter
    protected final PluginConfig projectConfig;
    @Getter
    protected final String gerritUserEmail;
    @Getter
    protected final GerritApi gerritApi;

    public ConfigCore(
            OneOffRequestContext context,
            GerritApi gerritApi,
            PluginConfig globalConfig,
            PluginConfig projectConfig,
            String gerritUserEmail,
            Account.Id userId
    ) {
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

    public String getString(String key, String defaultValue) {
        String value = projectConfig.getString(key);
        if (value != null) {
            return value;
        }
        return globalConfig.getString(key, defaultValue);
    }

    protected String getProjectGlobalString(String key, String defaultValue) {
        String globalValue = globalConfig.getString(key, defaultValue);
        String projectValue = projectConfig.getString(key, defaultValue);
        log.debug("Get project global string - globalConfig: {}, projectConfig: {}", globalValue,
                projectValue);
        String resultValue = joinWithComma(
                Stream.of(globalValue, projectValue)
                        .filter(s -> !s.isEmpty())
                        .map(TextUtils::unwrapQuotes)
                        .collect(Collectors.toSet())
        );
        return resultValue.isEmpty() ? "" : wrapQuotes(resultValue);
    }

    protected boolean isDefinedKey(Class<?> configClass, String key) {
        try {
            String configKey = PREFIX_KEY + convertCamelToSnakeCase(key).toUpperCase();
            log.debug("Checking if config key `{}` for {} is defined", configKey, key);
            Field field = configClass.getDeclaredField(configKey);
            String value = getFieldConfigValue(field);
            log.debug("Config key value: {}", value);
            return value.equals(key);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Error checking if config key `{}` is defined", key, e);
            return false;
        }
    }

    protected TreeMap<String, String> dumpConfigMap(Class<?> configClass) {
        log.debug("Start dumping config map");
        TreeMap<String, String> configMap = new TreeMap<>();
        try {
            for (Field field : configClass.getDeclaredFields()) {
                String fieldName = field.getName();
                log.debug("Processing dumping config field `{}`", fieldName);
                if (!fieldName.startsWith(PREFIX_KEY) ||
                        EXCLUDE_FROM_DUMP.contains(fieldName)) {
                    continue;
                }
                String fieldValue = getFieldConfigValue(field);
                String getterName = PREFIX_GETTER + capitalizeFirstLetter(fieldValue);
                String configValue;
                try {
                    configValue = configClass.getDeclaredMethod(getterName).invoke(this).toString();
                } catch (NoSuchMethodException e) {
                    log.debug("Config field `{}` lacking getter method `{}` is excluded from the dump",
                            fieldName, getterName);
                    continue;
                }
                log.debug("Config entities retrieved - Field Value: `{}`, Getter Name: `{}`, Config " +
                                "Value: `{}`", fieldValue, getterName, configValue);
                configMap.put(fieldValue, configValue);
            }
            return configMap;
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.info("Error retrieving configuration", e);
            return null;
        }
    }

    protected String getValidatedOrThrow(String key) {
        String value = projectConfig.getString(key);
        if (value == null) {
            value = globalConfig.getString(key);
        }
        if (value == null) {
            throw new RuntimeException(String.format(NOT_CONFIGURED_ERROR_MSG, key));
        }
        return value;
    }

    protected int getInt(String key, int defaultValue) {
        int valueForProject = projectConfig.getInt(key, defaultValue);
        // To avoid misinterpreting an undefined value as zero, a secondary check is performed by retrieving the value
        // as a String.
        if (valueForProject != defaultValue && valueForProject != 0
                && projectConfig.getString(key, "") != null) {
            return valueForProject;
        }
        return globalConfig.getInt(key, defaultValue);
    }

    protected boolean getBoolean(String key, boolean defaultValue) {
        boolean valueForProject = projectConfig.getBoolean(key, defaultValue);
        if (projectConfig.getString(key) != null) {
            return valueForProject;
        }
        return globalConfig.getBoolean(key, defaultValue);
    }

    protected Double getDouble(String key, Double defaultValue) {
        return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
    }

    protected List<String> splitConfig(String value) {
        return TextUtils.splitString(value);
    }

    protected List<String> splitConfig(String value, String delimiter) {
        return TextUtils.splitString(value, delimiter);
    }

    protected List<String> splitNarrativeConfig(String value) {
        return splitConfig(value, TextUtils.QUOTED_ITEM_COMMA_DELIMITED)
                .stream()
                .filter(s -> !s.isEmpty())
                .map(s -> StringUtils.appendIfMissing(StringUtils.strip(s, TextUtils.DOUBLE_QUOTES), "."))
                .collect(toList());
    }

    private String getFieldConfigValue(Field field) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(null).toString();
    }
}
