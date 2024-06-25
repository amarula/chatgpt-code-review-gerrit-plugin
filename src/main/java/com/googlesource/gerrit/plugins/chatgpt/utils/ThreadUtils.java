package com.googlesource.gerrit.plugins.chatgpt.utils;

public class ThreadUtils {
    public static void threadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        }
    }
}
