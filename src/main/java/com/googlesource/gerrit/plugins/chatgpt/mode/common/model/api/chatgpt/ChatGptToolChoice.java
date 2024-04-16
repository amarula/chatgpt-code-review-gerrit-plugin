package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import lombok.Data;

@Data
public class ChatGptToolChoice {
    private String type;
    private Function function;

    @Data
    public static class Function {
        private String name;
    }
}
