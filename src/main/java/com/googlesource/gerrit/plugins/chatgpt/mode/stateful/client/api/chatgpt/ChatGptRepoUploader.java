package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptFilesResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.createTempFileWithContent;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.sanitizeFilename;

@Slf4j
public class ChatGptRepoUploader extends ClientBase {
    private static final String FILENAME_PATTERN = "%s_%05d_";

    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;

    private String filenameBase;
    private int filenameIndex;

    public ChatGptRepoUploader(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config);
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
    }

    public List<String> uploadRepoFiles() throws OpenAiConnectionFailException {
        log.debug("Uploading repository files.");
        List<String> repoFiles = gitRepoFiles.getGitRepoFiles(config, change);
        List<String> filesIds = new ArrayList<>();
        filenameBase = sanitizeFilename(change.getProjectName());
        filenameIndex = 0;
        for(String repoFile : repoFiles) {
            Path repoPath = createTempFileWithContent(getIndexedFilename(), ".json", repoFile);
            log.debug("Uploading repository file `{}`", repoPath);
            ChatGptFile chatGptFile = new ChatGptFile(config);
            ChatGptFilesResponse chatGptFilesResponse = chatGptFile.uploadFile(repoPath);
            filesIds.add(chatGptFilesResponse.getId());
        }
        log.debug("IDs of the uploaded files: {}", filesIds);

        return filesIds;
    }

    private String getIndexedFilename() {
        filenameIndex++;
        return String.format(FILENAME_PATTERN, filenameBase, filenameIndex);
    }
}
