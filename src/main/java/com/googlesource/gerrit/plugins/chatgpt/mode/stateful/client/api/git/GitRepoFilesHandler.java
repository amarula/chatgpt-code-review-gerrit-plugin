package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git;

public class GitRepoFilesHandler {
    private static GitRepoFiles gitRepoFiles;

    public static synchronized GitRepoFiles getInstance() {
        return gitRepoFiles;
    }

    public static synchronized void createNewInstance(GitRepoFiles gitRepoFiles) {
        GitRepoFilesHandler.gitRepoFiles = gitRepoFiles;
    }

}
