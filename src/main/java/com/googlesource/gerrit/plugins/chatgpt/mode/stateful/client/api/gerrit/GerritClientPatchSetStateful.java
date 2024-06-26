package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.account.AccountCache;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptAssistant;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.gerrit.IGerritClientPatchSet;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler pluginDataHandler;

    @VisibleForTesting
    @Inject
    public GerritClientPatchSetStateful(
            Configuration config,
            AccountCache accountCache,
            GitRepoFiles gitRepoFiles,
            PluginDataHandler pluginDataHandler) {
        super(config, accountCache);
        this.gitRepoFiles = gitRepoFiles;
        this.pluginDataHandler = pluginDataHandler;
    }

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) {
        ChatGptAssistant chatGptAssistant = new ChatGptAssistant(config, change, gitRepoFiles, pluginDataHandler);
        chatGptAssistant.setupAssistant();

        return "";
    }

}
