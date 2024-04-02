package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatGptRequest {
    private String model;
    private boolean stream;
    private double temperature;
    private int seed;
    private List<ChatGptRequestMessage> messages;
    private List<Tool> tools;
    @SerializedName("tool_choice")
    private ToolChoice toolChoice;

    @Data
    public static class Tool {
        private String type;
        private Function function;

        @Data
        public static class Function {
            private String name;
            private String description;
            private Parameters parameters;

            @Data
            public static class Parameters {
                private String type;
                private Properties properties;
                private List<String> required;

                @Data
                public static class Properties {
                    private Property replies;
                    // Field `changeId` expected in the response to correspond with the PatchSet changeId in the request
                    private Field changeId;

                    @Data
                    public static class Property {
                        private String type;
                        private Item items;

                        @Data
                        public static class Item {
                            private String type;
                            private ObjectProperties properties;
                            private List<String> required;

                            @Data
                            public static class ObjectProperties {
                                private Field id;
                                private Field reply;
                                private Field score;
                                private Field relevance;
                                private Field repeated;
                                private Field conflicting;
                                private Field filename;
                                private Field lineNumber;
                                private Field codeSnippet;
                            }
                        }
                    }

                    @Data
                    public static class Field {
                        private String type;
                    }
                }
            }
        }
    }

    @Data
    public static class ToolChoice {
        private String type;
        private Function function;

        @Data
        public static class Function {
            private String name;
        }
    }

}
