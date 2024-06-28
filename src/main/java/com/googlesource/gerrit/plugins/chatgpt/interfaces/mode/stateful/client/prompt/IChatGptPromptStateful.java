package com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt;

import java.util.List;

public interface IChatGptPromptStateful {
    void addGptAssistantInstructions(List<String> instructions);
    String getDefaultGptAssistantDescription();
    String getDefaultGptAssistantInstructions();
    String getDefaultGptThreadReviewMessage(String patchSet);
    String getGptRequestDataPrompt();
    void setCommentEvent(boolean isCommentEvent);
}
