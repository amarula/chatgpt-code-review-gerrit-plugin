package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gerrit.entities.LabelId;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ApprovalInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPatchSetDetail;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

@Slf4j
public class GerritClientDetail {
    private static final SimpleDateFormat DATE_FORMAT = newFormat();

    private final Configuration config;

    private GerritPatchSetDetail gerritPatchSetDetail;

    public GerritClientDetail(Configuration config) {
        this.config = config;
    }

    public List<GerritComment> getMessages(GerritChange change) {
        loadPatchSetDetail(change);
        return gerritPatchSetDetail.getMessages();
    }

    public boolean isWorkInProgress(GerritChange change) {
        loadPatchSetDetail(change);
        return gerritPatchSetDetail.getWorkInProgress() != null && gerritPatchSetDetail.getWorkInProgress();
    }

    public GerritPermittedVotingRange getPermittedVotingRange(GerritChange change) {
        int gptAccountId = ChangeSetDataHandler.getInstance(change).getGptAccountId();
        loadPatchSetDetail(change);
        List<GerritPatchSetDetail.Permission> permissions = gerritPatchSetDetail.getLabels().getCodeReview().getAll();
        if (permissions == null) {
            log.debug("No limitations on the ChatGPT voting range were detected");
            return null;
        }
        for (GerritPatchSetDetail.Permission permission : permissions) {
            if (permission.getAccountId() == gptAccountId) {
                log.debug("PatchSet voting range detected for ChatGPT user: {}", permission.getPermittedVotingRange());
                return permission.getPermittedVotingRange();
            }
        }
        return null;
    }

    private void loadPatchSetDetail(GerritChange change) {
        if (gerritPatchSetDetail != null) {
            return;
        }
        try {
            gerritPatchSetDetail = getReviewDetail(change);
        }
        catch (Exception e) {
            log.error("Error retrieving PatchSet details", e);
        }
    }

    private GerritPatchSetDetail getReviewDetail(GerritChange change) throws Exception {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
          ChangeInfo info =
              config
                  .getGerritApi()
                  .changes()
                  .id(change.getProjectName(), change.getBranchNameKey().shortName(), change.getChangeKey().get())
                  .get();

          GerritPatchSetDetail detail = new GerritPatchSetDetail();
          detail.setWorkInProgress(info.workInProgress);
          Optional.ofNullable(info.labels)
              .map(Map::entrySet)
              .map(Set::stream)
              .flatMap(
                  labels ->
                      labels
                          .filter(label -> LabelId.CODE_REVIEW.equals(label.getKey()))
                          .map(GerritClientDetail::toLabels)
                          .findAny())
              .ifPresent(detail::setLabels);
          Optional.ofNullable(info.messages)
              .map(messages -> messages.stream().map(GerritClientDetail::toComment).collect(toList()))
              .ifPresent(detail::setMessages);

          return detail;
        }
    }

    private static GerritPatchSetDetail.Labels toLabels(Entry<String, LabelInfo> label) {
        List<GerritPatchSetDetail.Permission> permissions =
            Optional.ofNullable(label.getValue().all)
                .map(all -> all.stream().map(GerritClientDetail::toPermission).collect(toList()))
                .orElse(emptyList());
        GerritPatchSetDetail.CodeReview codeReview = new GerritPatchSetDetail.CodeReview();
        codeReview.setAll(permissions);
        GerritPatchSetDetail.Labels labels = new GerritPatchSetDetail.Labels();
        labels.setCodeReview(codeReview);
        return labels;
    }

    private static GerritPatchSetDetail.Permission toPermission(ApprovalInfo value) {
        GerritPatchSetDetail.Permission permission = new GerritPatchSetDetail.Permission();
        permission.setValue(value.value);
        permission.setDate(toDateString(value.date));
        GerritPermittedVotingRange range =
            new GerritPermittedVotingRange();
        range.setMin(value.permittedVotingRange.min);
        range.setMax(value.permittedVotingRange.max);
        permission.setPermittedVotingRange(range);
        permission.setAccountId(value._accountId);
        return permission;
    }

    private static GerritComment toComment(ChangeMessageInfo message) {
        GerritComment comment = new GerritComment();
        comment.setAuthor(toAuthor(message.author));
        comment.setId(message.id);
        comment.setTag(message.tag);
        comment.setDate(toDateString(message.date));
        comment.setMessage(message.message);
        comment.setPatchSet(message._revisionNumber);
        return comment;
    }

    private static GerritComment.Author toAuthor(AccountInfo authorInfo ) {
        GerritComment.Author author = new GerritComment.Author();
        author.setAccountId(authorInfo._accountId);
        author.setName(authorInfo.name);
        author.setDisplayName(author.getDisplayName());
        author.setEmail(authorInfo.email);
        author.setUsername(author.getUsername());
        return author;
    }

    /**
     * Date format copied from {@link com.google.gerrit.json.SqlTimestampDeserializer}
     */
    private static String toDateString(Timestamp input) {
        return DATE_FORMAT.format(input) + "000000";
    }

    private static SimpleDateFormat newFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(true);
        return f;
    }
}
