package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git;

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
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class GitRepoFiles {
    public static final String REPO_PATTERN = "git/%s.git";

    public String getGitRepoFiles(GerritChange change) {
        String repoPath = String.format(REPO_PATTERN, change.getProjectNameKey().toString());
        try {
            Repository repository = openRepository(repoPath);
            Map<String, String> filesWithContent = listFilesWithContent(repository);

            return getGson().toJson(filesWithContent);
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to retrieve files in master branch: ", e);
        }
    }

    public Map<String, String> listFilesWithContent(Repository repository) throws IOException, GitAPIException {
        Map<String, String> filesWithContent = new HashMap<>();
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
                    ObjectId objectId = treeWalk.getObjectId(0);
                    byte[] bytes = reader.open(objectId).getBytes();
                    String content = new String(bytes, StandardCharsets.UTF_8); // Assumes text files with UTF-8 encoding
                    filesWithContent.put(path, content);
                }
            }
        }
        return filesWithContent;
    }

    public Repository openRepository(String path) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(path))
                .readEnvironment()
                .findGitDir()
                .setMustExist(true)
                .build();
    }

}
