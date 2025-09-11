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

package com.googlesource.gerrit.plugins.reviewai.openai;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AIPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.openai.client.prompt.IOpenAIPrompt;
import com.googlesource.gerrit.plugins.reviewai.aibackend.openai.model.api.openai.OpenAIListResponse;
import com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static com.googlesource.gerrit.plugins.reviewai.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static com.googlesource.gerrit.plugins.reviewai.utils.GsonUtils.getGson;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class OpenAIReviewTaskSpecificTest extends OpenAIReviewTestBase {
  private static final String OPENAI_REVIEW_ASSISTANT_ID = "asst_TEST_REVIEW_ASSISTANT_ID";
  private static final String OPENAI_COMMIT_MESSAGE_ASSISTANT_ID =
      "asst_TEST_COMMIT_MESSAGE_ASSISTANT_ID";
  private static final String OPENAI_REVIEW_RUN_ID = "run_TEST_REVIEW_RUN_ID";
  private static final String OPENAI_COMMIT_MESSAGE_RUN_ID = "run_TEST_COMMIT_MESSAGE_RUN_ID";
  private static final String SECOND_CALL = "second-call";

  public OpenAIReviewTaskSpecificTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Override
  protected void initGlobalAndProjectConfig() {
    super.initGlobalAndProjectConfig();

    when(globalConfig.getBoolean(Mockito.eq("taskSpecificAssistants"), Mockito.anyBoolean()))
        .thenReturn(true);
  }

  @Override
  protected void setupMockRequests() throws RestApiException {
    super.setupMockRequests();

    setupMockRequestCreateAssistant(OPENAI_REVIEW_ASSISTANT_ID, Scenario.STARTED, SECOND_CALL);
    setupMockRequestCreateAssistant(OPENAI_COMMIT_MESSAGE_ASSISTANT_ID, SECOND_CALL);
    setupMockRequestCreateRun(
        OPENAI_REVIEW_ASSISTANT_ID, OPENAI_REVIEW_RUN_ID, Scenario.STARTED, SECOND_CALL);
    setupMockRequestCreateRun(
        OPENAI_COMMIT_MESSAGE_ASSISTANT_ID, OPENAI_COMMIT_MESSAGE_RUN_ID, SECOND_CALL);
    setupMockRequestRetrieveRunStepsFromBody(
        filterOutSubsetRunStepsResponse(1, 2), OPENAI_REVIEW_RUN_ID);
    setupMockRequestRetrieveRunStepsFromBody(
        filterOutSubsetRunStepsResponse(0, 1), OPENAI_COMMIT_MESSAGE_RUN_ID);
  }

  private String filterOutSubsetRunStepsResponse(int from, int to) {
    OpenAIListResponse runStepsResponse =
        GsonUtils.jsonToClass(
            readTestFile(RESOURCE_OPENAI_PATH + "openAIRunStepsResponse.json"),
            OpenAIListResponse.class);
    runStepsResponse.getData().get(0).getStepDetails().getToolCalls().subList(from, to).clear();

    return getGson().toJson(runStepsResponse);
  }

  @Test
  public void patchSetCreatedOrUpdated() throws Exception {
    String reviewMessageCode =
        getReviewMessage(RESOURCE_OPENAI_PATH + "openAIRunStepsResponse.json", 0);
    String reviewMessageCommitMessage =
        getReviewMessage(RESOURCE_OPENAI_PATH + "openAIRunStepsResponse.json", 1);

    handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

    IOpenAIPrompt openAIPromptOpenAICommitMessage =
        AIPromptFactory.getOpenAIPromptOpenAI(
            config, changeSetData, getGerritChange(), getCodeContextPolicy());
    String reviewPrompt =
        openAIPromptOpenAICommitMessage.getDefaultGptThreadReviewMessage(formattedPatchContent);

    ArgumentCaptor<ReviewInput> captor = testRequestSent();
    // Ensure that each of code and commit message reviews are performed only once
    Assert.assertEquals(1, getCapturedComments(captor, "test_file_1.py").size());
    Assert.assertEquals(1, getCapturedComments(captor, GERRIT_PATCH_SET_FILENAME).size());

    Assert.assertEquals(reviewPrompt, requestContent);
    Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
    Assert.assertEquals(
        reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
  }
}
