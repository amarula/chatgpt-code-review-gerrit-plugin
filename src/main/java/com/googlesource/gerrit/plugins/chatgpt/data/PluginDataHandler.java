package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Singleton
public class PluginDataHandler {
    private final Path configFile;
    private final Properties configProperties = new Properties();

    @Inject
    public PluginDataHandler(@com.google.gerrit.extensions.annotations.PluginData Path pluginDataDir) {
        this.configFile = pluginDataDir.resolve("plugin.config");
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

    public String getValue(String key) {
        return configProperties.getProperty(key);
    }

    public synchronized void removeValue(String key) {
        if (configProperties.containsKey(key)) {
            configProperties.remove(key);
            storeProperties();
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
