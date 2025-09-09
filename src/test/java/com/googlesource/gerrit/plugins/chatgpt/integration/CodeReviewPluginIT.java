package com.googlesource.gerrit.plugins.chatgpt.integration;

import com.google.gerrit.server.account.AccountCache;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit.GerritClientReview;
import com.googlesource.gerrit.plugins.chatgpt.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.model.review.ReviewBatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.when;

@Ignore(
    "This test suite is designed to demonstrate how to test the Gerrit and GPT interfaces in a real environment. "
        + "It is not intended to be executed during the regular build process")
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CodeReviewPluginIT {
  @Mock private Configuration config;

  @Mock protected PluginDataHandlerProvider pluginDataHandlerProvider;

  @InjectMocks private GerritClient gerritClient;

  @InjectMocks private AccountCache accountCache;

  @Test
  public void getPatchSet() throws Exception {
    when(config.getGerritUserName()).thenReturn("Your Gerrit username");

    String patchSet = gerritClient.getPatchSet("${changeId}");
    log.info("patchSet: {}", patchSet);
    assertNotNull(patchSet);
  }

  @Test
  public void setReview() throws Exception {
    ChangeSetData changeSetData =
        new ChangeSetData(1, config.getVotingMinScore(), config.getVotingMaxScore());
    Localizer localizer = new Localizer(config);
    when(config.getGerritUserName()).thenReturn("Your Gerrit username");

    List<ReviewBatch> reviewBatches = new ArrayList<>();
    reviewBatches.add(new ReviewBatch("message"));

    GerritClientReview gerritClientReview =
        new GerritClientReview(config, accountCache, pluginDataHandlerProvider, localizer);
    gerritClientReview.setReview(new GerritChange("Your changeId"), reviewBatches, changeSetData);
  }
}
