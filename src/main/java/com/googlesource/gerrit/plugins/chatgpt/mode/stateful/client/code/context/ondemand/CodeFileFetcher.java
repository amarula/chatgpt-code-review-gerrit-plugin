package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CodeFileFetcher extends ClientBase {
    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;

    private String basePathRegEx = "";

    public CodeFileFetcher(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config);
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
        if (!config.getCodeContextOnDemandBasePath().isEmpty()) {
            basePathRegEx = "^" + config.getCodeContextOnDemandBasePath() + "/";
        }
    }

    public String getFileContent(String filename) throws IOException {
        if (!basePathRegEx.isEmpty()) {
            filename = filename.replaceAll(basePathRegEx, "");
        }
        return gitRepoFiles.getFileContent(change, filename);
    }
}
