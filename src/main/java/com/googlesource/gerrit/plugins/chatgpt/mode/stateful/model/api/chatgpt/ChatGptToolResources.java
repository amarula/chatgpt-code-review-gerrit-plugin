package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatGptToolResources {
    @SerializedName("code_interpreter")
    private ChatGptFileIds codeInterpreter;
}
