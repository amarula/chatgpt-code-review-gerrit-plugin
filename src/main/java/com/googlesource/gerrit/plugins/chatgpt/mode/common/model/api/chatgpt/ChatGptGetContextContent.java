package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class ChatGptGetContextContent {
    private List<ChatGptGetContextItem> replies;
}
