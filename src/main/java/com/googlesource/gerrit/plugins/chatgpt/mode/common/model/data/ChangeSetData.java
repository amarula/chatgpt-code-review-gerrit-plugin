package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Data
@Slf4j
public class ChangeSetData {
    @NonNull
    private Integer gptAccountId;
    private String gptDataPrompt;
    private Integer commentPropertiesSize;
    @NonNull
    private Integer votingMinScore;
    @NonNull
    private Integer votingMaxScore;

    // Command variables
    private Boolean forcedReview = false;
    private Boolean forcedReviewLastPatchSet = false;
    private Boolean replyFilterEnabled = true;
    private Boolean debugReviewMode = false;
    private Boolean hideChatGptReview = false;
    private Set<String> directives = new HashSet<>();
    private String reviewSystemMessage;

    public Boolean shouldHideChatGptReview() {
        return hideChatGptReview && !forcedReview;
    }

    public Boolean shouldRequestChatGptReview() {
        return reviewSystemMessage == null && !shouldHideChatGptReview();
    }
}
