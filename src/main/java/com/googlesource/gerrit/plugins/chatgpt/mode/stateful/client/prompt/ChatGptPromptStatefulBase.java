package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public abstract class ChatGptPromptStatefulBase extends ChatGptPrompt implements IChatGptPromptStateful {
    public static String DEFAULT_GPT_ASSISTANT_NAME;
    public static String DEFAULT_GPT_ASSISTANT_DESCRIPTION;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS;
    public static String DEFAULT_GPT_MESSAGE_REVIEW;

    protected final ChangeSetData changeSetData;

    private final GerritChange change;

    public ChatGptPromptStatefulBase(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.isCommentEvent = change.getIsCommentEvent();
        // Avoid repeated loading of prompt constants
        if (DEFAULT_GPT_ASSISTANT_NAME == null) {
            loadDefaultPrompts("promptsStateful");
        }
    }

    public String getDefaultGptAssistantDescription() {
        return String.format(DEFAULT_GPT_ASSISTANT_DESCRIPTION, change.getProjectName());
    }

    public abstract void addGptAssistantInstructions(List<String> instructions);

    public abstract String getGptRequestDataPrompt();

    public String getDefaultGptAssistantInstructions() {
        List<String> instructions = new ArrayList<>(List.of(
                DEFAULT_GPT_SYSTEM_PROMPT + DOT,
                String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS, change.getProjectName())
        ));
        addGptAssistantInstructions(instructions);

        return joinWithSpace(instructions);
    }

    public String getDefaultGptThreadReviewMessage(String patchSet) {
        String gptRequestDataPrompt = getGptRequestDataPrompt();
        if (gptRequestDataPrompt != null && !gptRequestDataPrompt.isEmpty()) {
            log.debug("Request User Prompt retrieved: {}", gptRequestDataPrompt);
            return gptRequestDataPrompt;
        }
        else {
            return String.format(DEFAULT_GPT_MESSAGE_REVIEW, patchSet);
        }
    }
}
