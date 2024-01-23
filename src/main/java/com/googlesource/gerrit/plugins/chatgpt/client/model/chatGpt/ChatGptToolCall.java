package com.googlesource.gerrit.plugins.chatgpt.client.model.chatGpt;

import lombok.Data;

@Data
public class ChatGptToolCall {

    private String id;
    private String type;
    private Function function;

    @Data
    public static class Function {
        private String name;
        private String arguments;
    }
}
