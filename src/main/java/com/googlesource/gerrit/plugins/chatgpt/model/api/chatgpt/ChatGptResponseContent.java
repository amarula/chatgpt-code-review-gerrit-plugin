package com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt;

import lombok.Data;

import java.util.List;

@Data
public class ChatGptResponseContent {
    private List<ChatGptReplyItem> replies;
    private String changeId;
}
