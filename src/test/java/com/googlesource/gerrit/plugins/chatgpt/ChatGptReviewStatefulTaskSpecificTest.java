package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.prompt.IChatGptPromptStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptListResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask.SupportedEvents;
import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptPromptFactory.getChatGptPromptStateful;
import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ChatGptReviewStatefulTaskSpecificTest extends ChatGptReviewStatefulTestBase {
    private static final String CHAT_GPT_REVIEW_ASSISTANT_ID = "asst_TEST_REVIEW_ASSISTANT_ID";
    private static final String CHAT_GPT_COMMIT_MESSAGE_ASSISTANT_ID = "asst_TEST_COMMIT_MESSAGE_ASSISTANT_ID";
    private static final String CHAT_GPT_REVIEW_RUN_ID = "run_TEST_REVIEW_RUN_ID";
    private static final String CHAT_GPT_COMMIT_MESSAGE_RUN_ID = "run_TEST_COMMIT_MESSAGE_RUN_ID";
    private static final String SECOND_CALL = "second-call";

    public ChatGptReviewStatefulTaskSpecificTest() {
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

        setupMockRequestCreateAssistant(CHAT_GPT_REVIEW_ASSISTANT_ID, Scenario.STARTED, SECOND_CALL);
        setupMockRequestCreateAssistant(CHAT_GPT_COMMIT_MESSAGE_ASSISTANT_ID, SECOND_CALL);
        setupMockRequestCreateRun(CHAT_GPT_REVIEW_ASSISTANT_ID, CHAT_GPT_REVIEW_RUN_ID, Scenario.STARTED, SECOND_CALL);
        setupMockRequestCreateRun(CHAT_GPT_COMMIT_MESSAGE_ASSISTANT_ID, CHAT_GPT_COMMIT_MESSAGE_RUN_ID, SECOND_CALL);
        setupMockRequestRetrieveRunStepsFromBody(filterOutSubsetRunStepsResponse(1, 2), CHAT_GPT_REVIEW_RUN_ID);
        setupMockRequestRetrieveRunStepsFromBody(filterOutSubsetRunStepsResponse(0, 1), CHAT_GPT_COMMIT_MESSAGE_RUN_ID);
    }

    private String filterOutSubsetRunStepsResponse(int from, int to) {
        ChatGptListResponse runStepsResponse = getGson().fromJson(
                readTestFile(RESOURCE_STATEFUL_PATH + "chatGptRunStepsResponse.json"),
                ChatGptListResponse.class
        );
        runStepsResponse.getData().get(0).getStepDetails().getToolCalls().subList(from, to).clear();

        return getGson().toJson(runStepsResponse);
    }

    @Test
    public void patchSetCreatedOrUpdated() throws Exception {
        String reviewMessageCode = getReviewMessage("__files/stateful/chatGptRunStepsResponse.json", 0);
        String reviewMessageCommitMessage = getReviewMessage("__files/stateful/chatGptRunStepsResponse.json", 1);

        handleEventBasedOnType(SupportedEvents.PATCH_SET_CREATED);

        IChatGptPromptStateful chatGptPromptStatefulCommitMessage =
                getChatGptPromptStateful(config, changeSetData, getGerritChange());
        String reviewPrompt = chatGptPromptStatefulCommitMessage.getDefaultGptThreadReviewMessage(formattedPatchContent);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();
        // Ensure that each of code and commit message reviews are performed only once
        Assert.assertEquals(1, getCapturedComments(captor, "test_file_1.py").size());
        Assert.assertEquals(1, getCapturedComments(captor, GERRIT_PATCH_SET_FILENAME).size());

        Assert.assertEquals(reviewPrompt, requestContent);
        Assert.assertEquals(reviewMessageCode, getCapturedMessage(captor, "test_file_1.py"));
        Assert.assertEquals(reviewMessageCommitMessage, getCapturedMessage(captor, GERRIT_PATCH_SET_FILENAME));
    }
}
