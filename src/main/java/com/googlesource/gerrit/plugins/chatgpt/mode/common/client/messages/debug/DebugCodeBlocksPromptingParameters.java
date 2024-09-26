package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.chatgpt.ChatGptParameters;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptClientStateful.ReviewAssistantStages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.distanceCodeDelimiter;

public abstract class DebugCodeBlocksPromptingParameters extends DebugCodeBlocksComposer {
    private final Configuration config;
    private final ChangeSetData changeSetData;
    private final GerritChange change;
    protected final LinkedHashMap<String, String> promptingParameters = new LinkedHashMap<>();

    protected IChatGptPromptStateful chatGptPromptStateful;

    public DebugCodeBlocksPromptingParameters(
            Localizer localizer,
            String titleKey,
            Configuration config,
            ChangeSetData changeSetData,
            GerritChange change
    ) {
        super(localizer, titleKey);
        this.config = config;
        this.change = change;
        this.changeSetData = changeSetData;
    }

    public String getDebugCodeBlock() {
        return super.getDebugCodeBlock(getPromptingParameters());
    }

    private List<String> getPromptingParameters() {
        switch (config.getGptMode()) {
            case stateful -> populateStatefulParameters();
            case stateless -> populateStatelessParameters();
        }
        return promptingParameters.entrySet().stream()
                .map(e -> getAsTitle(e.getKey()) + "\n" + distanceCodeDelimiter(e.getValue()) + "\n")
                .collect(Collectors.toList());
    }

    protected void populateStatefulParameters() {
        ChatGptParameters chatGptParameters = new ChatGptParameters(config, false);
        chatGptPromptStateful = getChatGptPromptStateful(
                config,
                changeSetData,
                change,
                ReviewAssistantStages.REVIEW_CODE
        );
        if (chatGptParameters.shouldSpecializeAssistants()) {
            populateStatefulSpecializedCodeReviewParameters();
            chatGptPromptStateful = getChatGptPromptStateful(
                    config,
                    changeSetData,
                    change,
                    ReviewAssistantStages.REVIEW_COMMIT_MESSAGE
            );
            populateStatefulSpecializedCommitMessageReviewParameters();
        }
        else {
            populateStatefulReviewParameters();
        }
    }

    protected abstract void populateStatefulSpecializedCodeReviewParameters();

    protected abstract void populateStatefulSpecializedCommitMessageReviewParameters();

    protected abstract void populateStatefulReviewParameters();

    protected abstract void populateStatelessParameters();
}
