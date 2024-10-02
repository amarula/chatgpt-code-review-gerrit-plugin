package com.googlesource.gerrit.plugins.chatgpt.errors;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Slf4j
public class ErrorMessageHandler {
    private final Configuration config;
    private final Localizer localizer;

    public ErrorMessageHandler(Configuration config, Localizer localizer) {
        this.config = config;
        this.localizer = localizer;
    }

    public void updateErrorMessages(List<String> messages) {
        Set<String> unknownEnumSettings = config.getUnknownEnumSettings();
        log.debug("Updating error messages with: {}", unknownEnumSettings);
        if (unknownEnumSettings.isEmpty()) {
            return;
        }
        messages.addAll(unknownEnumSettings.stream()
                .map(m -> String.format(localizer.getText("message.config.unknown.enum.error"), m))
                .toList()
        );
        log.debug("Updated error messages: {}", messages);
    }
}
