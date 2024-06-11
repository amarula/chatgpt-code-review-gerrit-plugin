package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatGptThreadMessageResponse extends ChatGptResponse {
    private List<Content> content;

    @Data
    public static class Content {
        private String type;
        private Text text;

        @Data
        public static class Text {
            private String value;
        }
    }
}
