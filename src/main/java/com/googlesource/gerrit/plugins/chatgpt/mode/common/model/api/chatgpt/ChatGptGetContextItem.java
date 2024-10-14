package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true)
public class ChatGptGetContextItem extends ChatGptDialogueItem {
    private String requestType;
    private String otherDescription;
    private String entityCategory;
    private String contextRequiredEntity;
}
