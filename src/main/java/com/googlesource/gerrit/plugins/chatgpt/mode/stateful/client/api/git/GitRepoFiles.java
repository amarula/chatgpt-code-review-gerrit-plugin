package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.matchesExtensionList;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import java.util.ArrayList;

@Slf4j
public class GitRepoFiles {
    public static final String REPO_PATTERN = "git/%s.git";

    private List<String> enabledFileExtensions;
    private long maxChunkSize;

    public List<String> getGitRepoFiles(Configuration config, GerritChange change) {
        maxChunkSize = 1024 * 1024 * (long) config.getGptUploadedChunkSizeMb();
        enabledFileExtensions = config.getEnabledFileExtensions();
        log.debug("Open Repo from {}", change.getProjectNameKey());
        String repoPath = String.format(REPO_PATTERN, change.getProjectNameKey().toString());
        try {
            Repository repository = openRepository(repoPath);
            log.debug("Open Repo path: {}", repoPath);
            List<Map<String, String>> chunks = listFilesWithContent(repository);

            return chunks.stream()
                    .map(chunk -> getGson().toJson(chunk))
                    .collect(Collectors.toList());
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to retrieve files in master branch: ", e);
        }
    }

    private List<Map<String, String>> listFilesWithContent(Repository repository) throws IOException, GitAPIException {
        List<Map<String, String>> chunks = new ArrayList<>();
        Map<String, String> currentChunk = new HashMap<>();
        long currentChunkSize = 0;

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
                    ObjectId objectId = treeWalk.getObjectId(0);
                    byte[] bytes = reader.open(objectId).getBytes();
                    long fileSize = bytes.length;

                    if (currentChunkSize + fileSize > maxChunkSize) {
                        chunks.add(currentChunk);
                        currentChunk = new HashMap<>();
                        currentChunkSize = 0;
                    }

                    String content = new String(bytes, StandardCharsets.UTF_8); // Assumes text files with UTF-8 encoding
                    currentChunk.put(path, content);
                    currentChunkSize += fileSize;
                    log.debug("Repo File loaded: {}", path);
                }
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk);
                }
            }
        }
        return chunks;
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
