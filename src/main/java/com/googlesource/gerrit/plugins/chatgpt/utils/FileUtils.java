package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class FileUtils {
    public static InputStreamReader getInputStreamReader(String filename) {
        return new InputStreamReader(Objects.requireNonNull(
                FileUtils.class.getClassLoader().getResourceAsStream(filename)), StandardCharsets.UTF_8);
    }

    public static Path createTempFileWithContent(String prefix, String suffix, String content) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile(prefix, suffix);
            Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        tempFile.toFile().deleteOnExit();

        return tempFile;
    }

    public static boolean matchesExtensionList(String filename, List<String> extensions) {
        int extIndex = filename.lastIndexOf('.');
        return extIndex >= 1 && extensions.contains(filename.substring(extIndex));
    }

    public static String sanitizeFilename (String filename) {
        // Replace any characters that are invalid in filenames (especially slashes) with a "+"
        return filename.replaceAll("[^-_a-zA-Z0-9]", "+");
    }
}
