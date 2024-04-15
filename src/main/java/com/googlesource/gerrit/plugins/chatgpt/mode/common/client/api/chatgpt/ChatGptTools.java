package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptRequest;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

public class ChatGptTools {
    public static ChatGptRequest retrieveTools() {
        ChatGptRequest tools;
        try (InputStreamReader reader = FileUtils.getInputStreamReader("Config/tools.json")) {
            tools = getGson().fromJson(reader, ChatGptRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ChatGPT request tools", e);
        }
        return tools;
    }

}
