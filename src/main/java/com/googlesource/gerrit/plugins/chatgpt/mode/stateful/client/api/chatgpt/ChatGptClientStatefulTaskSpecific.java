package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptResponseContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ChatGptClientStatefulTaskSpecific extends ChatGptClientStateful implements IChatGptClient {
    private static final List<ReviewAssistantStages> TASK_SPECIFIC_ASSISTANT_STAGES = List.of(
            ReviewAssistantStages.REVIEW_CODE,
            ReviewAssistantStages.REVIEW_COMMIT_MESSAGE
    );

    @VisibleForTesting
    @Inject
    public ChatGptClientStatefulTaskSpecific(
            Configuration config,
            ICodeContextPolicy codeContextPolicy,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        super(config, codeContextPolicy, pluginDataHandlerProvider);
        log.debug("Initialized ChatGptClientStatefulTaskSpecific.");
    }

    public ChatGptResponseContent ask(ChangeSetData changeSetData, GerritChange change, String patchSet)
            throws OpenAiConnectionFailException {
        log.debug("Task-specific ChatGPT ask method called with changeId: {}", change.getFullChangeId());
        if (change.getIsCommentEvent()) {
            return super.ask(changeSetData, change, patchSet);
        }
        List<ChatGptResponseContent> chatGptResponseContents = new ArrayList<>();
        for (ReviewAssistantStages assistantStage : TASK_SPECIFIC_ASSISTANT_STAGES) {
            changeSetData.setReviewAssistantStage(assistantStage);
            log.debug("Processing stage: {}", assistantStage);
            chatGptResponseContents.add(super.ask(changeSetData, change, patchSet));
        }
        return mergeResponses(chatGptResponseContents);
    }

    private ChatGptResponseContent mergeResponses(List<ChatGptResponseContent> chatGptResponseContents) {
        log.debug("Merging responses from different task-specific stages.");
        ChatGptResponseContent mergedResponse = chatGptResponseContents.remove(0);
        for (ChatGptResponseContent chatGptResponseContent : chatGptResponseContents) {
            List<ChatGptReplyItem> replies = chatGptResponseContent.getReplies();
            if (replies != null) {
                mergedResponse.getReplies().addAll(replies);
            }
            else {
                mergedResponse.setMessageContent(chatGptResponseContent.getMessageContent());
            }
        }
        log.debug("Merged response content: {}", mergedResponse.getMessageContent());
        return mergedResponse;
    }
}
