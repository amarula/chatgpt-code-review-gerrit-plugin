package com.googlesource.gerrit.plugins.chatgpt.model.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptReplyItem extends ChatGptDialogueItem {
    private String reply;
}
