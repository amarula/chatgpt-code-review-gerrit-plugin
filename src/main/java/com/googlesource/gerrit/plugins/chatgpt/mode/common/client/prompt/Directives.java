package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;

public class Directives {
    private final ChangeSetData changeSetData;
    private String directive;

    public Directives(GerritChange change) {
        changeSetData = ChangeSetDataHandler.getInstance(change);
    }

    public void addDirective(String directive) {
        this.directive = directive.trim();
    }

    public void copyDirectiveToSettings() {
        if (!directive.isEmpty()) {
            changeSetData.getDirectives().add(directive);
        }
    }

}
