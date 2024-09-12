package com.googlesource.gerrit.plugins.chatgpt.config.entry;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.config.entry.IConfigEntry;

public class ConfigEntryFactory {

    public static IConfigEntry getConfigEntry(String key) {
        return Configuration.LIST_TYPE_ENTRY_KEYS.contains(key) ?
                new ConfigEntryList(key) :
                new ConfigEntryString(key);
    }
}
