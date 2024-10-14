package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.code.context.ondemand;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@ToString(callSuper = true)
public class GetContextOutputItem extends ChatGptGetContextItem {
    private String definition;
    private String body;
}
