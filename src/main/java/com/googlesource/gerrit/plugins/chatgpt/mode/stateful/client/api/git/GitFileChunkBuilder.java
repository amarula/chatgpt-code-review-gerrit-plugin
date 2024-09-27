package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.git.FileEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GitFileChunkBuilder {
    private final long maxChunkSize;
    private final List<Map<String, String>> chunks = new ArrayList<>();

    private Map<String, String> currentChunk = new LinkedHashMap<>();
    private long currentChunkSize = 0;

    public GitFileChunkBuilder(Configuration config) {
        maxChunkSize = 1024 * 1024 * (long) config.getGptUploadedChunkSizeMb();
    }

    public void addFiles(String path, List<FileEntry> fileEntries) {
        // Start a new chunk if adding the entire directory would exceed maxChunkSize
        long dirSize = fileEntries.stream().mapToLong(FileEntry::getSize).sum();
        if (currentChunkSize + dirSize > maxChunkSize) {
            startNewChunk();
        }
        for (FileEntry fe : fileEntries) {
            log.debug("ChunkBuilder - Processing file: {}", fe.getPath());
            if (fe.getSize() > maxChunkSize) {
                // Handle large file
                startNewChunk();
                Map<String, String> singleFileChunk = new LinkedHashMap<>();
                updateChunk(singleFileChunk, fe);
                chunks.add(singleFileChunk);
                log.warn("File {} exceeds maxChunkSize, added in its own chunk", fe.getPath());
            } else {
                if (currentChunkSize + fe.getSize() > maxChunkSize) {
                    startNewChunk();
                }
                updateChunk(currentChunk, fe);
            }
        }
        startNewChunk();
    }

    public List<Map<String, String>> getChunks() {
        startNewChunk();
        return chunks;
    }

    private void updateChunk(Map<String, String> chunk, FileEntry fe) {
        chunk.put(fe.getPath(), fe.getContent());
        currentChunkSize += fe.getSize();
    }

    private void startNewChunk() {
        if (currentChunk.isEmpty()) {
            return;
        }
        log.debug("Started new chunk");
        chunks.add(currentChunk);
        currentChunk = new LinkedHashMap<>();
        currentChunkSize = 0;
    }
}
