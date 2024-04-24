package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistant;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {
    private final GitRepoFiles gitRepoFiles;

    @Inject
    public GerritClientPatchSetStateful(Configuration config, GitRepoFiles gitRepoFiles) {
        super(config);
        this.gitRepoFiles = gitRepoFiles;
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) {
        ChatGptAssistant chatGptAssistant = new ChatGptAssistant(config, change, gitRepoFiles);
        chatGptAssistant.setupAssistant();

        return "";
    }

}
