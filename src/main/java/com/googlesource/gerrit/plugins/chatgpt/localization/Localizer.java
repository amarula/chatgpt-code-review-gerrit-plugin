package com.googlesource.gerrit.plugins.chatgpt.localization;

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
public class Localizer {
    private final ResourceBundle resourceBundle;

    @Inject
    public Localizer(Configuration config) {
        resourceBundle = ResourceBundle.getBundle("localization.localTexts", config.getLocaleDefault());
        log.debug("ResourceBundle initialized with locale: {}", config.getLocaleDefault());
    }

    public String getText(String key) {
        String text = resourceBundle.getString(key);
        log.debug("Retrieved text for key '{}': {}", key, text);
        return text;
    }

    public List<String> filterProperties(String prefix, String suffix) {
        // Return a list of values of the keys starting with "prefix" and ending with "suffix"
        return resourceBundle.keySet().stream()
                .filter(key -> (prefix == null || key.startsWith(prefix)) &&
                        (suffix == null || key.endsWith(suffix)))
                .map(resourceBundle::getString)
                .collect(Collectors.toList());
    }
}
