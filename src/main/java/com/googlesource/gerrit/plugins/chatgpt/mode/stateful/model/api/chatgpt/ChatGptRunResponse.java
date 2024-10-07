package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptToolCall;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class ChatGptRunResponse extends ChatGptResponse {
    @SerializedName("required_action")
    private RequiredAction requiredAction;

    @Data
    public static class RequiredAction {
        @SerializedName("submit_tool_outputs")
        private SubmitToolOutputs submitToolOutputs;
        private String type;

        @Data
        public static class SubmitToolOutputs {
            @SerializedName("tool_calls")
            private List<ChatGptToolCall> toolCalls;
        }
    }
}
