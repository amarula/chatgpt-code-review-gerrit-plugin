package com.googlesource.gerrit.plugins.chatgpt.config;

public abstract class ConfigEntryBase {
    protected final String key;

    public ConfigEntryBase(String key) {
        this.key = key;
    }
}
