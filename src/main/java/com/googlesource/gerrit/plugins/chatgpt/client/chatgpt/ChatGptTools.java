package com.googlesource.gerrit.plugins.chatgpt.client.chatgpt;

import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.chatgpt.model.chatgpt.ChatGptRequest;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;

class ChatGptTools {
    private final static Gson gson = new Gson();

    public static ChatGptRequest retrieveTools() {
        ChatGptRequest tools;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("Config/tools.json")) {
            tools = gson.fromJson(reader, ChatGptRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ChatGPT request tools", e);
        }
        return tools;
    }

}
