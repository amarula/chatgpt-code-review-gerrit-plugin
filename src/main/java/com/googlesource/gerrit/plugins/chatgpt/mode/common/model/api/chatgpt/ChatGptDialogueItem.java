package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ChatGptDialogueItem {
    protected Integer id;
    protected String filename;
    protected Integer lineNumber;
    protected String codeSnippet;
}
