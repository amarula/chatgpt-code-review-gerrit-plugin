package com.googlesource.gerrit.plugins.chatgpt.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.model.settings.Settings;
import com.googlesource.gerrit.plugins.chatgpt.settings.DynamicSettings;

public class Directives {
    private final Settings settings;
    private String directive;

    public Directives(GerritChange change) {
        settings = DynamicSettings.getInstance(change);
    }

    public void addDirective(String directive) {
        this.directive = directive.trim();
    }

    public void copyDirectiveToSettings() {
        if (!directive.isEmpty()) {
            settings.getDirectives().add(directive);
        }
    }

}
