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

package com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt;

import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAiRequestMessage;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.CommentData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AiHistory extends AiComment {
  private final Set<String> messagesExcludedFromHistory;
  @Getter private final HashMap<String, GerritComment> commentMap;
  private final HashMap<String, GerritComment> patchSetCommentMap;
  private final Set<String> patchSetCommentAdded;
  private final List<GerritComment> patchSetComments;
  private final int revisionBase;

  private boolean filterActive;

  public AiHistory(
      Configuration config,
      ChangeSetData changeSetData,
      GerritClientData gerritClientData,
      Localizer localizer) {
    super(config, changeSetData, localizer);
    CommentData commentData = gerritClientData.getCommentData();
    messagesExcludedFromHistory =
        Set.of(Settings.GERRIT_DEFAULT_MESSAGE_DONE, localizer.getText("message.empty.review"));
    commentMap = commentData.getCommentMap();
    patchSetCommentMap = commentData.getPatchSetCommentMap();
    patchSetComments = retrievePatchSetComments(gerritClientData);
    revisionBase = gerritClientData.getOneBasedRevisionBase();
    patchSetCommentAdded = new HashSet<>();
    log.debug("AiHistory initialized with comments and revision base: {}", revisionBase);
  }

  public List<OpenAiRequestMessage> retrieveHistory(
      GerritComment commentProperty, boolean filterActive) {
    this.filterActive = filterActive;
    log.debug("Retrieving history for commentProperty with filterActive={}", filterActive);
    if (commentProperty.isPatchSetComment()) {
      return retrievePatchSetMessageHistory();
    } else {
      log.debug("Retrieving history for comment: {}", commentProperty);
      return retrieveMessageHistory(commentProperty);
    }
  }

  public List<OpenAiRequestMessage> retrieveHistory(GerritComment commentProperty) {
    return retrieveHistory(commentProperty, false);
  }

  private List<GerritComment> retrievePatchSetComments(GerritClientData gerritClientData) {
    List<GerritComment> detailComments = gerritClientData.getDetailComments();
    // Normalize detailComments by setting the `update` field to match `date`
    detailComments.forEach(record -> record.setUpdated(record.getDate()));
    // Join the comments from patchSetCommentMap with detailComments
    List<GerritComment> patchSetComments =
        Stream.concat(patchSetCommentMap.values().stream(), detailComments.stream())
            .collect(Collectors.toList());
    sortPatchSetComments(patchSetComments);
    log.debug("Patch set comments sorted by update datetime: {}", patchSetComments);
    return patchSetComments;
  }

  private void sortPatchSetComments(List<GerritComment> patchSetComments) {
    Comparator<GerritComment> byDateUpdated =
        (GerritComment o1, GerritComment o2) -> {
          String dateTime1 = o1.getUpdated();
          String dateTime2 = o2.getUpdated();
          if (dateTime1 == null && dateTime2 == null) return 0;
          if (dateTime1 == null) return 1;
          if (dateTime2 == null) return -1;

          return dateTime1.compareTo(dateTime2);
        };
    patchSetComments.sort(byDateUpdated);
  }

  private String getRoleFromComment(GerritComment currentComment) {
    return isFromAssistant(currentComment)
        ? Settings.OPENAI_ROLE_ASSISTANT
        : Settings.OPENAI_ROLE_USER;
  }

  private List<OpenAiRequestMessage> retrieveMessageHistory(GerritComment currentComment) {
    List<OpenAiRequestMessage> messageHistory = new ArrayList<>();
    log.debug("Retrieving message history for currentComment: {}", currentComment);
    while (currentComment != null) {
      log.debug("Processing comment: {}", currentComment);
      addMessageToHistory(messageHistory, currentComment);
      currentComment = commentMap.get(currentComment.getInReplyTo());
    }
    // Reverse the history sequence so that the oldest message appears first and the newest message
    // is last
    Collections.reverse(messageHistory);
    log.debug("Final message history: {}", messageHistory);
    return messageHistory;
  }

  private List<OpenAiRequestMessage> retrievePatchSetMessageHistory() {
    List<OpenAiRequestMessage> messageHistory = new ArrayList<>();
    log.debug("Retrieving patch set message history.");
    for (GerritComment patchSetComment : patchSetComments) {
      if (patchSetComment.isAutogenerated()) {
        continue;
      }
      if (!isFromAssistant(patchSetComment)) {
        GerritComment patchSetLevelMessage = patchSetCommentMap.get(patchSetComment.getId());
        if (patchSetLevelMessage != null) {
          patchSetComment = patchSetLevelMessage;
        }
      }
      addMessageToHistory(messageHistory, patchSetComment);
    }
    log.debug("Final patch set message history: {}", messageHistory);
    return messageHistory;
  }

  private boolean isInactiveComment(GerritComment comment) {
    boolean isInactive =
        config.ignoreResolvedAiComments() && isFromAssistant(comment) && comment.isResolved()
            || config.ignoreOutdatedInlineComments()
                && comment.getOneBasedPatchSet() != revisionBase
                && !comment.isPatchSetComment();
    log.debug("Checking if comment is inactive: {}", isInactive);
    return isInactive;
  }

  private void addMessageToHistory(
      List<OpenAiRequestMessage> messageHistory, GerritComment comment) {
    log.debug("Adding message to history - comment: {}", comment);
    String messageContent = getCleanedMessage(comment);
    log.debug("Cleaned message content: {}", messageContent);
    boolean shouldNotProcessComment =
        messageContent.isEmpty()
            || messagesExcludedFromHistory.contains(messageContent)
            || patchSetCommentAdded.contains(messageContent)
            || filterActive && isInactiveComment(comment);

    if (shouldNotProcessComment) {
      log.debug(
          "Message not added to history - messagesExcludedFromHistory: {} - patchSetCommentAdded: {} - "
              + "isInactiveComment: {}",
          messagesExcludedFromHistory.contains(messageContent),
          patchSetCommentAdded.contains(messageContent),
          isInactiveComment(comment));
      return;
    }
    patchSetCommentAdded.add(messageContent);

    OpenAiRequestMessage message =
        OpenAiRequestMessage.builder()
            .role(getRoleFromComment(comment))
            .content(messageContent)
            .build();
    log.debug("Message added to history: {}", message);
    messageHistory.add(message);
  }
}
