package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;

@Data
public abstract class ChatGptDialogueItem {
    protected Integer id;
    protected String filename;
    protected Integer lineNumber;
    protected String codeSnippet;
}
