package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Slf4j
public class FileUtils {
    public static InputStreamReader getInputStreamReader(String filename) {
        try {
            InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                    FileUtils.class.getClassLoader().getResourceAsStream(filename)), StandardCharsets.UTF_8);
            log.debug("Input stream reader created for file: {}", filename);
            return reader;
        } catch (NullPointerException e) {
            log.error("File not found or error reading the file: {}", filename, e);
            throw new RuntimeException("File not found or error reading the file: " + filename, e);
        }
    }

    public static Path createTempFileWithContent(String prefix, String suffix, String content) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile(prefix, suffix);
            Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
            log.debug("Temporary file created: {}", tempFile);
        } catch (IOException e) {
            log.error("Failed to create or write to temporary file: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        tempFile.toFile().deleteOnExit();

        return tempFile;
    }

    public static boolean matchesExtensionList(String filename, List<String> extensions) {
        boolean matches = extensions.contains(getExtension(filename));
        log.debug("Filename '{}' matches extension list: {}", filename, matches);
        return matches;
    }

    public static String sanitizeFilename (String filename) {
        String sanitized = filename.replaceAll("[^-_a-zA-Z0-9]", "+");
        log.debug("Original filename: '{}', Sanitized filename: '{}'", filename, sanitized);
        return sanitized;
    }

    public static String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex <= 0 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
