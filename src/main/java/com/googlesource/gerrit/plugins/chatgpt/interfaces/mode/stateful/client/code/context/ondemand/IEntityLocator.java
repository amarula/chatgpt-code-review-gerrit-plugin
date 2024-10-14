package com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextItem;

public interface IEntityLocator {
    String findDefinition(ChatGptGetContextItem chatGptGetContextItem);
}
