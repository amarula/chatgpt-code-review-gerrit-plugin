package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import lombok.Data;

@Data
public class ChatGptFilesResponse {
    String id;
    String filename;
    String status;
}
