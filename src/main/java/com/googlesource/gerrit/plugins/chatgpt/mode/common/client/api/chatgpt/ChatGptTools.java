package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolChoice;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptTools {
    public static ChatGptTool retrieveFormatRepliesTool() {
        ChatGptTool tools;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("config/formatRepliesTool.json")) {
            tools = getGson().fromJson(reader, ChatGptTool.class);
            log.debug("Successfully loaded format replies tool from JSON.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `format_replies` tool", e);
        }
        return tools;
    }

    public static ChatGptToolChoice retrieveFormatRepliesToolChoice() {
        ChatGptToolChoice toolChoice;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("config/formatRepliesToolChoice.json")) {
            toolChoice = getGson().fromJson(reader, ChatGptToolChoice.class);
            log.debug("Successfully loaded format replies tool choice from JSON.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `format_replies` tool choice", e);
        }
        return toolChoice;
    }
}
