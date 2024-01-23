package com.googlesource.gerrit.plugins.chatgpt.client.model.chatGpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptRequestItem extends ChatGptDialogueItem {
    private String request;
}
