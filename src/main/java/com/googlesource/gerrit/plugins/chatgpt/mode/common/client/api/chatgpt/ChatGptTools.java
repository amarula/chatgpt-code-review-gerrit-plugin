package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolChoice;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

public class ChatGptTools {
    public static ChatGptTool retrieveFormatRepliesTool() {
        ChatGptTool tools;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("Config/formatRepliesTool.json")) {
            tools = getGson().fromJson(reader, ChatGptTool.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `format_replies` tool", e);
        }
        return tools;
    }

    public static ChatGptToolChoice retrieveFormatRepliesToolChoice() {
        ChatGptToolChoice toolChoice;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("Config/formatRepliesToolChoice.json")) {
            toolChoice = getGson().fromJson(reader, ChatGptToolChoice.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `format_replies` tool choice", e);
        }
        return toolChoice;
    }

}
