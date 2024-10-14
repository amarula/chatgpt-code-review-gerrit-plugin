package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ChatGptReplyItem extends ChatGptDialogueItem {
    private String reply;
    private Integer score;
    private Double relevance;
    private boolean repeated;
    private boolean conflicting;
}
