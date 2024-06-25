package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Singleton
public class PluginDataHandler {
    private final Path configFile;
    private final Properties configProperties = new Properties();

    @Inject
    public PluginDataHandler(Path configFilePath) {
        this.configFile = configFilePath;
        try {
            loadOrCreateProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void setValue(String key, String value) {
        configProperties.setProperty(key, value);
        storeProperties();
    }

    public synchronized void setJsonValue(String key, Object value) {
        setValue(key, getGson().toJson(value));
    }

    public String getValue(String key) {
        return configProperties.getProperty(key);
    }

    public <T> Map<String, T> getJsonValue(String key, Class<T> clazz) {
        String value = getValue(key);
        if (value == null || value.isEmpty()) return null;
        Type typeOfMap = TypeToken.getParameterized(Map.class, String.class, clazz).getType();
        return getGson().fromJson(value, typeOfMap);
    }

    public synchronized void removeValue(String key) {
        if (configProperties.containsKey(key)) {
            configProperties.remove(key);
            storeProperties();
        }
    }

    public synchronized void destroy() {
        try {
            Files.deleteIfExists(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete the config file: " + configFile, e);
        }
    }

    private void storeProperties() {
        try (var output = Files.newOutputStream(configFile)) {
            configProperties.store(output, null);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadOrCreateProperties() throws IOException {
        if (Files.notExists(configFile)) {
            Files.createFile(configFile);
        } else {
            try (var input = Files.newInputStream(configFile)) {
                configProperties.load(input);
            }
        }
    }
}
