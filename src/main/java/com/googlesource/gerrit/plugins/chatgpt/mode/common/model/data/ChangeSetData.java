/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data;

import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt.ChatGptClientStateful.ReviewAssistantStages;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Data
@Slf4j
public class ChangeSetData {
  @NonNull private Integer gptAccountId;
  private String gptDataPrompt;
  private Integer commentPropertiesSize;
  private ReviewAssistantStages reviewAssistantStage = ReviewAssistantStages.REVIEW_CODE;
  private Boolean forcedStagedReview = false;
  @NonNull private Integer votingMinScore;
  @NonNull private Integer votingMaxScore;

  // Command variables
  private Boolean forcedReview = false;
  private Boolean forcedReviewLastPatchSet = false;
  private Boolean replyFilterEnabled = true;
  private Boolean debugReviewMode = false;
  private Boolean hideChatGptReview = false;
  private Boolean hideDynamicConfigMessage = false;
  private String reviewSystemMessage;

  public Boolean shouldHideChatGptReview() {
    return hideChatGptReview && !forcedReview;
  }

  public Boolean shouldRequestChatGptReview() {
    return reviewSystemMessage == null && !shouldHideChatGptReview();
  }
}
