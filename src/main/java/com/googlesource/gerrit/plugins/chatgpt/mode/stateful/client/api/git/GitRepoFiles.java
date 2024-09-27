package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.git.FileEntry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.matchesExtensionList;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class GitRepoFiles {
    public static final String REPO_PATTERN = "git/%s.git";

    private GitFileChunkBuilder gitFileChunkBuilder;
    private List<String> enabledFileExtensions;

    public List<String> getGitRepoFiles(Configuration config, GerritChange change) {
        gitFileChunkBuilder = new GitFileChunkBuilder(config);
        enabledFileExtensions = config.getEnabledFileExtensions();
        log.debug("Open Repo from {}", change.getProjectNameKey());
        String repoPath = String.format(REPO_PATTERN, change.getProjectNameKey().toString());
        try {
            Repository repository = openRepository(repoPath);
            log.debug("Open Repo path: {}", repoPath);
            List<Map<String, String>> chunkedFileContent = listFilesWithContent(repository);

            return chunkedFileContent.stream()
                    .map(chunk -> getGson().toJson(chunk))
                    .collect(Collectors.toList());
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to retrieve files in master branch: ", e);
        }
    }

    private List<Map<String, String>> listFilesWithContent(Repository repository) throws IOException, GitAPIException {
        Map<String, List<FileEntry>> dirFilesMap = new LinkedHashMap<>();

        try (ObjectReader reader = repository.newObjectReader();
             RevWalk revWalk = new RevWalk(repository)) {
            ObjectId lastCommitId = repository.resolve(Constants.R_HEADS + "master");
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            RevTree tree = commit.getTree();

            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(TreeFilter.ANY_DIFF);

                while (treeWalk.next()) {
                    String path = treeWalk.getPathString();
                    if (!matchesExtensionList(path, enabledFileExtensions)) continue;
                    int lastSlashIndex = path.lastIndexOf('/');
                    String dirPath = (lastSlashIndex != -1) ? path.substring(0, lastSlashIndex) : "";
                    ObjectId objectId = treeWalk.getObjectId(0);
                    byte[] bytes = reader.open(objectId).getBytes();
                    long fileSize = bytes.length;
                    String content = new String(bytes, StandardCharsets.UTF_8); // Assumes text files with UTF-8 encoding

                    dirFilesMap.computeIfAbsent(dirPath, k -> new ArrayList<>())
                            .add(new FileEntry(path, content, fileSize));
                    log.debug("Repo File loaded: {}", path);
                }
            }
        }
        for (Map.Entry<String, List<FileEntry>> entry : dirFilesMap.entrySet()) {
            String dirPath = entry.getKey();
            log.debug("File from dirFilesMap processed: {}", dirPath);
            List<FileEntry> fileEntries = entry.getValue();
            gitFileChunkBuilder.addFiles(dirPath, fileEntries);
        }

        return gitFileChunkBuilder.getChunks();
    }

    private Repository openRepository(String path) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(path))
                .readEnvironment()
                .findGitDir()
                .setMustExist(true)
                .build();
    }
}
