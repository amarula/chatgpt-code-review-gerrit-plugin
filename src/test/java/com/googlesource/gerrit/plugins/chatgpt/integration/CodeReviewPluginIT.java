package com.googlesource.gerrit.plugins.chatgpt.integration;

import com.google.gerrit.server.account.AccountCache;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClientReview;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review.ReviewBatch;
import com.googlesource.gerrit.plugins.chatgpt.mode.interfaces.client.api.chatgpt.IChatGptClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateless.client.prompt.ChatGptPromptStateless;
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

@Ignore("This test suite is designed to demonstrate how to test the Gerrit and GPT interfaces in a real environment. " +
        "It is not intended to be executed during the regular build process")
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class CodeReviewPluginIT {
    @Mock
    private Configuration config;

    @InjectMocks
    private GerritClient gerritClient;

    @InjectMocks
    private IChatGptClient chatGptClient;

    @InjectMocks
    private AccountCache accountCache;

    @Test
    public void sayHelloToGPT() throws Exception {
        ChangeSetData changeSetData = new ChangeSetData(1, config.getVotingMinScore(), config.getMaxReviewFileSize());
        ChatGptPromptStateless chatGptPromptStateless = new ChatGptPromptStateless(config, true);
        when(config.getGptDomain()).thenReturn(Configuration.OPENAI_DOMAIN);
        when(config.getGptToken()).thenReturn("Your GPT token");
        when(config.getGptModel()).thenReturn(Configuration.DEFAULT_GPT_MODEL);
        when(chatGptPromptStateless.getGptSystemPrompt()).thenReturn(ChatGptPromptStateless.DEFAULT_GPT_SYSTEM_PROMPT);

        String answer = chatGptClient.ask(config, changeSetData, new GerritChange(""), "hello");
        log.info("answer: {}", answer);
        assertNotNull(answer);
    }

    @Test
    public void getPatchSet() throws Exception {
        when(config.getGerritUserName()).thenReturn("Your Gerrit username");

        String patchSet = gerritClient.getPatchSet("${changeId}");
        log.info("patchSet: {}", patchSet);
        assertNotNull(patchSet);
    }

    @Test
    public void setReview() throws Exception {
        when(config.getGerritUserName()).thenReturn("Your Gerrit username");

        List<ReviewBatch> reviewBatches = new ArrayList<>();
        reviewBatches.add(new ReviewBatch());
        reviewBatches.get(0).setContent("message");

        GerritClientReview gerritClientReview = new GerritClientReview(config, accountCache);
        gerritClientReview.setReview(new GerritChange("Your changeId"), reviewBatches);
    }
}
