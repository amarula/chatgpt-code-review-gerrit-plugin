package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptToolOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptRunToolOutputHandler extends ClientBase {
    private static final List<String> ON_DEMAND_FUNCTION_NAMES = List.of("get_context", "multi_tool_use");

    private final ChatGptRun chatGptRun;

    public ChatGptRunToolOutputHandler(
            Configuration config,
            ChatGptRun chatGptRun
    ) {
        super(config);
        this.chatGptRun = chatGptRun;
    }

    public void submitToolOutput(List<ChatGptToolCall> chatGptToolCalls) throws OpenAiConnectionFailException {
        List<ChatGptToolOutput> toolOutputs = new ArrayList<>();
        log.info("chatGptToolCall: {}", chatGptToolCalls);
        for (ChatGptToolCall chatGptToolCall : chatGptToolCalls) {
            toolOutputs.add(ChatGptToolOutput.builder()
                    .toolCallId(chatGptToolCall.getId())
                    .output(getOutput(chatGptToolCall))
                    .build());
        }
        log.info("toolOutput: {}", toolOutputs);
        chatGptRun.submitToolOutputs(toolOutputs);
    }

    private String getOutput(ChatGptToolCall chatGptToolCall) {
        if (ON_DEMAND_FUNCTION_NAMES.contains(chatGptToolCall.getFunction().getName())) {
            // Placeholder string, will be replaced by logic to calculate code context based on ChatGPT request
            return "CONTEXT PROVIDED";
        }
        return "";
    }
}
