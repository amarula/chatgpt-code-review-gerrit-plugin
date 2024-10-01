package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class ChatGptResponseContent {
    private List<ChatGptReplyItem> replies;
    private String changeId;
    @NonNull
    private String messageContent;
    private String responseFunctionName;
}
