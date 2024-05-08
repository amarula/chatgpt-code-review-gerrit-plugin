package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.gerrit;

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
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

import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.COMMIT_MESSAGE_FILTER_OUT_PREFIXES;

@Slf4j
public class GerritClientPatchSetStateful extends GerritClientPatchSet implements IGerritClientPatchSet {
    private final GitRepoFiles gitRepoFiles;
    private final PluginDataHandler pluginDataHandler;

    private GerritChange change;

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

    public String getPatchSet(ChangeSetData changeSetData, GerritChange change) throws Exception {
        this.change = change;
        ChatGptAssistant chatGptAssistant = new ChatGptAssistant(config, change, gitRepoFiles, pluginDataHandler);
        chatGptAssistant.setupAssistant();

        return getPatchFromGerrit();
    }

    private String getPatchFromGerrit() throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            String gerritPatch = config
                .getGerritApi()
                .changes()
                .id(
                        change.getProjectName(),
                        change.getBranchNameKey().shortName(),
                        change.getChangeKey().get())
                .current()
                .patch()
                .asString();
            log.debug("Gerrit Patch retrieved: {}", gerritPatch);

            return filterPatch(gerritPatch);
        }
    }

    private String filterPatch(String gerritPatch) {
        // Remove Patch heading up to the Change-Id annotation
        Pattern CONFIG_ID_HEADING_PATTERN = Pattern.compile(
                "^.*?" + COMMIT_MESSAGE_FILTER_OUT_PREFIXES.get("CHANGE_ID") + " " + change.getChangeKey().get(),
                Pattern.DOTALL
        );
        return CONFIG_ID_HEADING_PATTERN.matcher(gerritPatch).replaceAll("");
    }

}
