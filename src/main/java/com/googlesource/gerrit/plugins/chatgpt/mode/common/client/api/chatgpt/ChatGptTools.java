package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptTool;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolChoice;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.convertCamelToSnakeCase;

@Slf4j
public class ChatGptTools {
    public enum Functions {
        formatReplies,
        getContext
    }

    private static final String FILENAME_TOOL_FORMAT = "config/%sTool.json";
    private static final String FILENAME_TOOL_CHOICE_FORMAT = "config/%sToolChoice.json";

    private final String functionName;

    public ChatGptTools(Functions function) {
        functionName = function.name();
    }

    public static String getSnakeCaseFunctionName(Functions functionName) {
        return convertCamelToSnakeCase(functionName.name());
    }

    public ChatGptTool retrieveFunctionTool() {
        ChatGptTool tools;
        try (InputStreamReader reader = FileUtils.getInputStreamReader(
                String.format(FILENAME_TOOL_FORMAT, functionName)
        )) {
            tools = getGson().fromJson(reader, ChatGptTool.class);
            log.debug("Successfully loaded format replies tool from JSON.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `" + functionName + "` tool", e);
        }
        return tools;
    }

    public ChatGptToolChoice retrieveFunctionToolChoice() {
        ChatGptToolChoice toolChoice;
        try (InputStreamReader reader = FileUtils.getInputStreamReader(
                String.format(FILENAME_TOOL_CHOICE_FORMAT, functionName)
        )) {
            toolChoice = getGson().fromJson(reader, ChatGptToolChoice.class);
            log.debug("Successfully loaded format replies tool choice from JSON.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data for ChatGPT `" + functionName + "` tool choice", e);
        }
        return toolChoice;
    }
}
