package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Singleton
@Slf4j
public class PluginDataHandler {
    private final Path configFile;
    private final Properties configProperties = new Properties();

    @Inject
    public PluginDataHandler(Path configFilePath) {
        this.configFile = configFilePath;
        try {
            log.debug("Loading or creating properties file at: {}", configFilePath);
            loadOrCreateProperties();
        } catch (IOException e) {
            log.error("Failed to load or create properties", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void setValue(String key, String value) {
        log.debug("Setting value for key: {} with value: {}", key, value);
        configProperties.setProperty(key, value);
        storeProperties();
    }

    public synchronized void setJsonValue(String key, Object value) {
        log.debug("Setting JSON value for key: {}", key);
        setValue(key, getGson().toJson(value));
    }

    public String getValue(String key) {
        log.debug("Getting value for key: {}", key);
        return configProperties.getProperty(key);
    }

    public <T> Map<String, T> getJsonValue(String key, Class<T> clazz) {
        log.debug("Getting JSON value for key: {}", key);
        String value = getValue(key);
        if (value == null || value.isEmpty()) {
            log.debug("No value found for key: {}", key);
            return null;
        }
        Type typeOfMap = TypeToken.getParameterized(Map.class, String.class, clazz).getType();
        return getGson().fromJson(value, typeOfMap);
    }

    public synchronized void removeValue(String key) {
        log.debug("Removing value for key: {}", key);
        if (configProperties.containsKey(key)) {
            configProperties.remove(key);
            storeProperties();
        }
    }

    public synchronized void destroy() {
        log.debug("Destroying configuration file at: {}", configFile);
        try {
            Files.deleteIfExists(configFile);
        } catch (IOException e) {
            log.error("Failed to delete the config file: " + configFile, e);
            throw new RuntimeException("Failed to delete the config file: " + configFile, e);
        }
    }

    private void storeProperties() {
        log.debug("Storing properties to file: {}", configFile);
        try (var output = Files.newOutputStream(configFile)) {
            configProperties.store(output, null);
        }
        catch (IOException e) {
            log.error("Failed to store properties", e);
            throw new RuntimeException(e);
        }
    }

    private void loadOrCreateProperties() throws IOException {
        log.debug("Checking existence of the configuration file: {}", configFile);
        if (Files.notExists(configFile)) {
            log.debug("Configuration file not found, creating new one at: {}", configFile);
            Files.createFile(configFile);
        }
        else {
            log.debug("Loading properties from file: {}", configFile);
            try (var input = Files.newInputStream(configFile)) {
                configProperties.load(input);
            }
        }
    }
}
