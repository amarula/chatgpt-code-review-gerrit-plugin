package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptFilesResponse extends ChatGptResponse {
    private String filename;
}
