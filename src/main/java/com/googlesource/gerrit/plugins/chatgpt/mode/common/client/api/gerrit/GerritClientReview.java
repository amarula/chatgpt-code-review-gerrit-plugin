package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.common.net.HttpHeaders;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.UriResourceLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.MessageSanitizer.sanitizeChatGptMessage;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.EMPTY_REVIEW_MESSAGE;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static java.net.HttpURLConnection.HTTP_OK;

@Slf4j
public class GerritClientReview extends GerritClientAccount {
    public GerritClientReview(Configuration config) {
        super(config);
    }

    public void setReview(String fullChangeId, List<ReviewBatch> reviewBatches, Integer reviewScore) throws Exception {
        GerritReview reviewMap = buildReview(reviewBatches, reviewScore);
        if (reviewMap.getComments() == null && reviewMap.getMessage() == null) {
            return;
        }
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritSetReviewUri(fullChangeId));
        log.debug("Set-Review uri: {}", uri);
        String auth = generateBasicAuth(config.getGerritUserName(),
                config.getGerritPassword());
        String json = getGson().toJson(reviewMap);
        log.debug("Set-Review JSON: {}", json);
        HttpRequest request = HttpRequest.newBuilder()
                .header(HttpHeaders.AUTHORIZATION, auth)
                .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .uri(uri)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClientWithRetry.execute(request);

        if (response.statusCode() != HTTP_OK) {
            log.error("Review setting failed with status code: {}", response.statusCode());
        }
    }

    public void setReview(String fullChangeId, List<ReviewBatch> reviewBatches) throws Exception {
        setReview(fullChangeId, reviewBatches, null);
    }

    private GerritReview buildReview(List<ReviewBatch> reviewBatches, Integer reviewScore) {
        GerritReview reviewMap = new GerritReview();
        Map<String, List<GerritComment>> comments = new HashMap<>();
        for (ReviewBatch reviewBatch : reviewBatches) {
            String message = sanitizeChatGptMessage(reviewBatch.getContent());
            if (message.trim().isEmpty()) {
                log.info("Empty message from review not submitted.");
                continue;
            }
            boolean unresolved;
            String filename = reviewBatch.getFilename();
            List<GerritComment> filenameComments = comments.getOrDefault(filename, new ArrayList<>());
            GerritComment filenameComment = new GerritComment();
            filenameComment.setMessage(message);
            if (reviewBatch.getLine() != null || reviewBatch.getRange() != null) {
                filenameComment.setLine(reviewBatch.getLine());
                filenameComment.setRange(reviewBatch.getRange());
                filenameComment.setInReplyTo(reviewBatch.getId());
                unresolved = !config.getInlineCommentsAsResolved();
            }
            else {
                unresolved = !config.getPatchSetCommentsAsResolved();
            }
            filenameComment.setUnresolved(unresolved);
            filenameComments.add(filenameComment);
            comments.putIfAbsent(filename, filenameComments);
        }
        if (comments.isEmpty()) {
            reviewMap.setMessage(EMPTY_REVIEW_MESSAGE);
        }
        else {
            reviewMap.setComments(comments);
        }
        if (reviewScore != null) {
            reviewMap.setLabels(new GerritReview.Labels(reviewScore));
        }
        return reviewMap;
    }

}
