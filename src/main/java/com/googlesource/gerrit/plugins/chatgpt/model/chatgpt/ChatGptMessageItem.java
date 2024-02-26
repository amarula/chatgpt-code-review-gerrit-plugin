package com.googlesource.gerrit.plugins.chatgpt.model.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptMessageItem extends ChatGptDialogueItem {
    private String request;
    private List<ChatGptRequestMessage> history;
}
