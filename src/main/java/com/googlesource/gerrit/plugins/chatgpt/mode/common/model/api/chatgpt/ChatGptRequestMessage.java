package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatGptRequestMessage {
    private String role;
    private String content;
    // PatchSet changeId passed in the request
    private String changeId;
}
