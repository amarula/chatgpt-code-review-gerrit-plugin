package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import lombok.Data;

import java.util.List;

@Data
public class ChatGptListResponse {
    private String object;
    private List<ChatGptRunStepsResponse> data;
}
