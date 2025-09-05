/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.endpoint.ChatGptRun;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.CodeContextBuilder;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptToolOutput;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptRunToolOutputHandler extends ChatGptClientBase {
    // ChatGPT may occasionally return the fixed string "multi_tool_use" as the function name when multiple tools are
    // utilized.
    private static final List<String> ON_DEMAND_FUNCTION_NAMES = List.of("get_context", "multi_tool_use");

    private final ChatGptRun chatGptRun;
    private final CodeContextBuilder codeContextBuilder;

    private List<ChatGptToolCall> chatGptToolCalls;

    public ChatGptRunToolOutputHandler(
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles,
            ChatGptRun chatGptRun
    ) {
        super(config);
        this.chatGptRun = chatGptRun;
        codeContextBuilder = new CodeContextBuilder(config, change, gitRepoFiles);
    }

    public void submitToolOutput(List<ChatGptToolCall> chatGptToolCalls) throws OpenAiConnectionFailException {
        this.chatGptToolCalls = chatGptToolCalls;
        List<ChatGptToolOutput> toolOutputs = new ArrayList<>();
        log.debug("ChatGpt Tool Calls: {}", chatGptToolCalls);
        for (int i = 0; i < chatGptToolCalls.size(); i++) {
            toolOutputs.add(ChatGptToolOutput.builder()
                    .toolCallId(chatGptToolCalls.get(i).getId())
                    .output(getOutput(i))
                    .build());
        }
        log.debug("ChatGpt Tool Outputs: {}", toolOutputs);
        chatGptRun.submitToolOutputs(toolOutputs);
    }

    private String getOutput(int i) {
        ChatGptToolCall.Function function = getFunction(chatGptToolCalls, i);
        if (ON_DEMAND_FUNCTION_NAMES.contains(function.getName())) {
            ChatGptGetContextContent getContextContent = getArgumentAsType(
                    chatGptToolCalls,
                    i,
                    ChatGptGetContextContent.class
            );
            log.debug("ChatGpt `get_context` Response Content: {}", getContextContent);
            return codeContextBuilder.buildCodeContext(getContextContent);
        }
        return "";
    }
}
