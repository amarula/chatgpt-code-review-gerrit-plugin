package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Singleton
@Slf4j
public class PluginDataHandlerBaseProvider implements Provider<PluginDataHandler> {
    private static final String PATH_SUFFIX = ".data";
    private static final String PATH_GLOBAL = "global";

    private final Path defaultPluginDataPath;

    @Inject
    public PluginDataHandlerBaseProvider(@com.google.gerrit.extensions.annotations.PluginData Path defaultPluginDataPath) {
        this.defaultPluginDataPath = defaultPluginDataPath;
        log.debug("PluginDataHandlerBaseProvider initialized with default plugin data path: {}", defaultPluginDataPath);
    }

    public PluginDataHandler get(String path) {
        return new PluginDataHandler(defaultPluginDataPath.resolve(path + PATH_SUFFIX));
    }

    @Override
    public PluginDataHandler get() {
        log.debug("Retrieving global PluginDataHandler");
        return get(PATH_GLOBAL);
    }
}
