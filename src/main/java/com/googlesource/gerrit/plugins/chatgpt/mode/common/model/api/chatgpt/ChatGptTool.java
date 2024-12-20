package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Data
public class ChatGptTool {
    @NonNull
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
                            private Field requestType;
                            private Field otherDescription;
                            private Field entityCategory;
                            private Field contextRequiredEntity;
                        }
                    }
                }

                @Data
                public static class Field {
                    private String type;
                    private String description;
                    @SerializedName("enum")
                    private List<String> enumeration;
                }
            }
        }
    }
}
