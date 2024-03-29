package com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptReplyItem extends ChatGptDialogueItem {
    private String reply;
    private Integer score;
    private Double relevance;
    private boolean repeated;
    private boolean conflicting;
}
