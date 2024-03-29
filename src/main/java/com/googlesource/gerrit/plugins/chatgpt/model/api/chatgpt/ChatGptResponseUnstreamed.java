package com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt;

import lombok.Data;

import java.util.List;

@Data
public class ChatGptResponseUnstreamed {
    private List<MessageChoice> choices;

    @Data
    public static class MessageChoice {
        private ChatGptResponseMessage message;
    }

}
