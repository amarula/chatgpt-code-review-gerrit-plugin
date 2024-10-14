package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
public class ChatGptMessageItem extends ChatGptDialogueItem {
    private String request;
    private List<ChatGptRequestMessage> history;

    public void appendToRequest(String appended) {
        request += appended;
    }
}
