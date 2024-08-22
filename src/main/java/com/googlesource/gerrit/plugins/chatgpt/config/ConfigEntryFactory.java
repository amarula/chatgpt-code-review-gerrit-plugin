package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.interfaces.config.IConfigEntry;

import java.util.Set;

public class ConfigEntryFactory {
    // Config entry keys with list values
    private static final Set<String> LIST_TYPE_ENTRY_KEYS = Set.of(
            Configuration.KEY_DIRECTIVES
    );

    public static IConfigEntry getConfigEntry(String key) {
        return LIST_TYPE_ENTRY_KEYS.contains(key) ?
                new ConfigEntryList(key) :
                new ConfigEntryString(key);
    }
}
