package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

public class Directives {
    private final ChangeSetData changeSetData;
    private String directive;

    public Directives(ChangeSetData changeSetData) {
        this.changeSetData = changeSetData;
    }

    public void addDirective(String directive) {
        this.directive = directive.trim();
    }

    public void copyDirectiveToSettings() {
        if (directive != null && !directive.isEmpty()) {
            changeSetData.getDirectives().add(directive);
        }
    }
}
