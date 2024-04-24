package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistant;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {

    @Inject
    public GerritClientPatchSetStateful(Configuration config) {
        super(config);
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) {
        ChatGptAssistant chatGptAssistant = new ChatGptAssistant(config, change);
        chatGptAssistant.setupAssistant();

        return "";
    }

}
