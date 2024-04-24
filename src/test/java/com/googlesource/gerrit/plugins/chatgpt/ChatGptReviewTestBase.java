package com.googlesource.gerrit.plugins.chatgpt;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.accounts.AccountApi;
import com.google.gerrit.extensions.api.accounts.Accounts;
import com.google.gerrit.extensions.api.accounts.Accounts.QueryRequest;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.api.changes.ChangeApi.CommentsRequest;
import com.google.gerrit.extensions.api.changes.Changes;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.json.OutputFormat;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.TypeLiteral;
import com.googlesource.gerrit.plugins.chatgpt.config.ConfigCreator;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventListenerHandler;
import com.googlesource.gerrit.plugins.chatgpt.listener.GerritListener;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.NonNull;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.google.gerrit.extensions.client.ChangeKind.REWORK;
import static com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.UriResourceLocator.*;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatGptReviewTestBase {
    protected static final Path basePath = Paths.get("src/test/resources");
    protected static final String GERRIT_AUTH_BASE_URL = "http://localhost:9527";
    protected static final int GERRIT_GPT_ACCOUNT_ID = 1000000;
    protected static final String GERRIT_GPT_USERNAME = "gpt";
    protected static final int GERRIT_USER_ACCOUNT_ID = 1000001;
    protected static final String GERRIT_USER_ACCOUNT_NAME = "Test";
    protected static final String GERRIT_USER_ACCOUNT_EMAIL = "test@example.com";
    protected static final String GERRIT_USER_USERNAME = "test";
    protected static final String GERRIT_USER_PASSWORD = "test";
    protected static final String GERRIT_USER_GROUP = "Test";
    protected static final String GPT_TOKEN = "tk-test";
    protected static final String GPT_DOMAIN = "http://localhost:9527";
    protected static final Project.NameKey PROJECT_NAME = Project.NameKey.parse("myProject");
    protected static final Change.Key CHANGE_ID = Change.Key.parse("myChangeId");
    protected static final BranchNameKey BRANCH_NAME = BranchNameKey.create(PROJECT_NAME, "myBranchName");
    protected static final boolean GPT_STREAM_OUTPUT = true;
    protected static final long TEST_TIMESTAMP = 1699270812;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9527);

    @Mock
    protected GitRepoFiles gitRepoFiles;

    @Mock
    protected PluginDataHandler pluginDataHandler;

    @Mock
    protected OneOffRequestContext context;
    @Mock
    protected GerritApi gerritApi;
    @Mock
    protected Changes changesMock;
    @Mock
    protected ChangeApi changeApiMock;
    @Mock
    protected RevisionApi revisionApiMock;
    @Mock
    protected CommentsRequest commentsRequestMock;

    protected PluginConfig globalConfig;
    protected PluginConfig projectConfig;
    protected Configuration config;
    protected GerritClient gerritClient;
    protected PatchSetReviewer patchSetReviewer;
    protected ConfigCreator mockConfigCreator;
    protected List<LoggedRequest> loggedRequests;
    protected JsonArray prompts;

    @Before
    public void before() throws NoSuchProjectException, RestApiException {
        initGlobalAndProjectConfig();
        initConfig();
        setupMockRequests();
        initComparisonContent();
        initTest();
    }

    protected GerritChange getGerritChange() {
        return new GerritChange(PROJECT_NAME, BRANCH_NAME, CHANGE_ID);
    }

    protected void initGlobalAndProjectConfig() {
        globalConfig = mock(PluginConfig.class);
        Answer<Object> returnDefaultArgument = invocation -> {
            // Return the second argument (i.e., the Default value) passed to the method
            return invocation.getArgument(1);
        };

        // Mock the Global Config values not provided by Default
        when(globalConfig.getString("gptToken")).thenReturn(GPT_TOKEN);

        // Mock the Global Config values to the Defaults passed as second arguments of the `get*` methods.
        when(globalConfig.getString(Mockito.anyString(), Mockito.anyString())).thenAnswer(returnDefaultArgument);
        when(globalConfig.getInt(Mockito.anyString(), Mockito.anyInt())).thenAnswer(returnDefaultArgument);
        when(globalConfig.getBoolean(Mockito.anyString(), Mockito.anyBoolean())).thenAnswer(returnDefaultArgument);

        // Mock the Global Config values that differ from the ones provided by Default
        when(globalConfig.getString(Mockito.eq("gptDomain"), Mockito.anyString()))
                .thenReturn(GPT_DOMAIN);

        projectConfig = mock(PluginConfig.class);

        // Mock the Project Config values
        when(projectConfig.getBoolean(Mockito.eq("isEnabled"), Mockito.anyBoolean())).thenReturn(true);
    }

    protected void initConfig() {
        config = new Configuration(context, gerritApi, globalConfig, projectConfig, "gpt@email.com", Account.id(1000000));
    }

    protected void setupMockRequests() throws RestApiException {
        String fullChangeId = getGerritChange().getFullChangeId();

        Accounts accountsMock = mockGerritAccountsRestEndpoint();
        // Mock the behavior of the gerritAccountIdUri request
        mockGerritAccountsQueryApiCall(accountsMock, GERRIT_GPT_USERNAME, GERRIT_GPT_ACCOUNT_ID);

        // Mock the behavior of the gerritAccountIdUri request
        mockGerritAccountsQueryApiCall(accountsMock, GERRIT_USER_USERNAME, GERRIT_USER_ACCOUNT_ID);

        // Mock the behavior of the gerritAccountGroups request
        mockGerritAccountGroupsApiCall(accountsMock, GERRIT_USER_ACCOUNT_ID);

        mockGerritChangeApiRestEndpoint();
        // Mock the behavior of the gerritPatchSetRevisionsUri request
        WireMock.stubFor(WireMock.get(gerritPatchSetRevisionsUri(fullChangeId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody("{\"revisions\":{\"aa5be5ebb80846475ec4dfe43e0799eb73c6415a\":{}}}")));

        // Mock the behavior of the gerritGetPatchSetDetailUri request
        WireMock.stubFor(WireMock.get(gerritGetPatchSetDetailUri(fullChangeId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("gerritPatchSetDetail.json")));
        mockGerritChangeDetailsApiCall();

        // Mock the behavior of the gerritPatchSet comments request
        WireMock.stubFor(WireMock.get(gerritGetAllPatchSetCommentsUri(fullChangeId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBodyFile("gerritPatchSetComments.json")));
        mockGerritChangeCommentsApiCall();

        // Mock the behavior of the postReview request
        WireMock.stubFor(WireMock.post(gerritSetReviewUri(fullChangeId))
                .willReturn(WireMock.aResponse()
                        .withStatus(HTTP_OK)));
    }

    private Accounts mockGerritAccountsRestEndpoint() {
        Accounts accountsMock = mock(Accounts.class);
        when(gerritApi.accounts()).thenReturn(accountsMock);
        return accountsMock;
    }

    private void mockGerritAccountsQueryApiCall(
        Accounts accountsMock, String username, int expectedAccountId) throws RestApiException {
        QueryRequest queryRequestMock = mock(QueryRequest.class);
        when(accountsMock.query(username)).thenReturn(queryRequestMock);
        when(queryRequestMock.get()).thenReturn(List.of(new AccountInfo(expectedAccountId)));
    }

    private void mockGerritAccountGroupsApiCall(Accounts accountsMock, int accountId)
        throws RestApiException {
        Gson gson = OutputFormat.JSON.newGson();
        List<GroupInfo> groups =
            gson.fromJson(
                readTestFile("__files/gerritAccountGroups.json"),
                new TypeLiteral<List<GroupInfo>>() {}.getType());
        AccountApi accountApiMock = mock(AccountApi.class);
        when(accountsMock.id(accountId)).thenReturn(accountApiMock);
        when(accountApiMock.getGroups()).thenReturn(groups);
    }

    private void mockGerritChangeDetailsApiCall() throws RestApiException {
        ChangeInfo changeInfo = readTestFileToClass("__files/gerritPatchSetDetail.json", ChangeInfo.class);
        when(changeApiMock.get()).thenReturn(changeInfo);
    }

    private void mockGerritChangeCommentsApiCall() throws RestApiException {
        Map<String, List<CommentInfo>> comments =
            readTestFileToType(
                "__files/gerritPatchSetComments.json",
                new TypeLiteral<Map<String, List<CommentInfo>>>() {}.getType());
        when(changeApiMock.commentsRequest()).thenReturn(commentsRequestMock);
        when(commentsRequestMock.get()).thenReturn(comments);
    }

    private void mockGerritChangeApiRestEndpoint() throws RestApiException {
        when(gerritApi.changes()).thenReturn(changesMock);
        when(changesMock.id(PROJECT_NAME.get(), BRANCH_NAME.shortName(), CHANGE_ID.get())).thenReturn(changeApiMock);
    }

    protected void initComparisonContent() {}

    protected <T> T readTestFileToClass(String filename, Class<T> clazz) {
        Gson gson = OutputFormat.JSON.newGson();
        return gson.fromJson(readTestFile(filename), clazz);
    }

    protected <T> T readTestFileToType(String filename, Type type) {
        Gson gson = OutputFormat.JSON.newGson();
        return gson.fromJson(readTestFile(filename), type);
    }

    protected String readTestFile(String filename) {
        try {
            return new String(Files.readAllBytes(basePath.resolve(filename)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected CompletableFuture<Void> handleEventBasedOnType(boolean isCommentEvent) {
        Consumer<Event> typeSpecificSetup = getTypeSpecificSetup(isCommentEvent);
        Event event = isCommentEvent ? mock(CommentAddedEvent.class) : mock(PatchSetCreatedEvent.class);
        setupCommonEventMocks((PatchSetEvent) event); // Apply common mock configurations
        typeSpecificSetup.accept(event);

        EventListenerHandler eventListenerHandler = new EventListenerHandler(patchSetReviewer, gerritClient);
        GerritListener gerritListener = new GerritListener(mockConfigCreator, eventListenerHandler, gitRepoFiles,
                pluginDataHandler);
        gerritListener.onEvent(event);

        return eventListenerHandler.getLatestFuture();
    }

    protected ArgumentCaptor<ReviewInput> testRequestSent() throws RestApiException {
        ArgumentCaptor<ReviewInput> reviewInputCaptor = ArgumentCaptor.forClass(ReviewInput.class); 
        verify(revisionApiMock).review(reviewInputCaptor.capture());
        JsonObject gptRequestBody = getGson().fromJson(patchSetReviewer.getChatGptClient().getRequestBody(),
                JsonObject.class);
        prompts = gptRequestBody.get("messages").getAsJsonArray();
        return reviewInputCaptor;
    }

    private void initTest () throws NoSuchProjectException {
        gerritClient = new GerritClient();
        patchSetReviewer = new PatchSetReviewer(gerritClient);
        mockConfigCreator = mock(ConfigCreator.class);
        when(mockConfigCreator.createConfig(ArgumentMatchers.any())).thenReturn(config);
    }

    private AccountAttribute createTestAccountAttribute() {
        AccountAttribute accountAttribute = new AccountAttribute();
        accountAttribute.name = GERRIT_USER_ACCOUNT_NAME;
        accountAttribute.username = GERRIT_USER_USERNAME;
        accountAttribute.email = GERRIT_USER_ACCOUNT_EMAIL;
        return accountAttribute;
    }

    private PatchSetAttribute createPatchSetAttribute() {
        PatchSetAttribute patchSetAttribute = new PatchSetAttribute();
        patchSetAttribute.kind = REWORK;
        patchSetAttribute.author = createTestAccountAttribute();
        return patchSetAttribute;
    }

    @NonNull
    private Consumer<Event> getTypeSpecificSetup(boolean isCommentEvent) {
        Consumer<Event> typeSpecificSetup;

        if (isCommentEvent) {
            typeSpecificSetup = event -> {
                CommentAddedEvent commentEvent = (CommentAddedEvent) event;
                commentEvent.author = this::createTestAccountAttribute;
                commentEvent.patchSet = this::createPatchSetAttribute;
                commentEvent.eventCreatedOn = TEST_TIMESTAMP;
                when(commentEvent.getType()).thenReturn("comment-added");
            };
        } else {
            typeSpecificSetup = event -> {
                PatchSetCreatedEvent patchEvent = (PatchSetCreatedEvent) event;
                patchEvent.patchSet = this::createPatchSetAttribute;
                when(patchEvent.getType()).thenReturn("patchset-created");
            };
        }
        return typeSpecificSetup;
    }

    private void setupCommonEventMocks(PatchSetEvent event) {
        when(event.getProjectNameKey()).thenReturn(PROJECT_NAME);
        when(event.getBranchNameKey()).thenReturn(BRANCH_NAME);
        when(event.getChangeKey()).thenReturn(CHANGE_ID);
    }

}
