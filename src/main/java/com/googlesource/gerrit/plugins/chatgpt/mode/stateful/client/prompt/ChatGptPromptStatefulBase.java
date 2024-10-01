package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.prompt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptCodeContextPolicies;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public abstract class ChatGptPromptStatefulBase extends ChatGptPrompt implements IChatGptPromptStateful {
    public static String DEFAULT_GPT_ASSISTANT_NAME;
    public static String DEFAULT_GPT_ASSISTANT_DESCRIPTION;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_NO_FILE_CONTEXT;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_FORMAT;
    public static String DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_RESPONSE_EXAMPLES;
    public static String DEFAULT_GPT_MESSAGE_REQUEST_RESEND_FORMATTED;
    public static String DEFAULT_GPT_MESSAGE_REVIEW;

    protected final ChangeSetData changeSetData;
    protected final GerritChange change;

    public ChatGptPromptStatefulBase(Configuration config, ChangeSetData changeSetData, GerritChange change) {
        super(config);
        this.changeSetData = changeSetData;
        this.change = change;
        this.isCommentEvent = change.getIsCommentEvent();
        loadDefaultPrompts("promptsStateful");
        log.debug("Initialized ChatGptPromptStatefulBase with change ID: {}", change.getFullChangeId());
    }

    public String getDefaultGptAssistantDescription() {
        String description = String.format(DEFAULT_GPT_ASSISTANT_DESCRIPTION, change.getProjectName());
        log.debug("Generated GPT Assistant Description: {}", description);
        return description;
    }

    public abstract void addGptAssistantInstructions(List<String> instructions);

    public abstract String getGptRequestDataPrompt();

    public String getDefaultGptAssistantInstructions() {
        List<String> instructions = new ArrayList<>(List.of(
                config.getGptSystemPromptInstructions(DEFAULT_GPT_SYSTEM_PROMPT_INSTRUCTIONS) + DOT
        ));
        addCodeContextPolicyAwareAssistantInstructions(instructions);
        addGptAssistantInstructions(instructions);
        String compiledInstructions = joinWithSpace(instructions);
        log.debug("Compiled GPT Assistant Instructions: {}", compiledInstructions);
        return compiledInstructions;
    }

    public String getDefaultGptThreadReviewMessage(String patchSet) {
        String gptRequestDataPrompt = getGptRequestDataPrompt();
        if (gptRequestDataPrompt != null && !gptRequestDataPrompt.isEmpty()) {
            log.debug("Request User Prompt retrieved: {}", gptRequestDataPrompt);
            return gptRequestDataPrompt;
        }
        else {
            String defaultMessage = String.format(DEFAULT_GPT_MESSAGE_REVIEW, patchSet);
            log.debug("Default Thread Review Message used: {}", defaultMessage);
            return defaultMessage;
        }
    }

    private void addCodeContextPolicyAwareAssistantInstructions(List<String> instructions) {
        if (config.getCodeContextPolicy() == ChatGptCodeContextPolicies.CodeContextPolicies.ON_DEMAND) {
            return;
        }
        String contextAwareInstructions = switch (config.getCodeContextPolicy()) {
            case NONE -> DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_NO_FILE_CONTEXT;
            case UPLOAD_ALL -> String.format(DEFAULT_GPT_ASSISTANT_INSTRUCTIONS_FILE_CONTEXT, change.getProjectName());
            default -> throw new IllegalStateException("Unexpected value: " + config.getCodeContextPolicy());
        };
        instructions.add(contextAwareInstructions);
    }
}
