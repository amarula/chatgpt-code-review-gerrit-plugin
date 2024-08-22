package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.json.OutputFormat;
import com.google.gson.Gson;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.listener.EventHandlerTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.config.Configuration.KEY_DIRECTIVES;
import static com.googlesource.gerrit.plugins.chatgpt.config.dynamic.DynamicConfigManager.KEY_DYNAMIC_CONFIG;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.backslashDoubleQuotes;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TemplateUtils.renderTemplate;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.sortTextLines;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommandTest extends ChatGptReviewStatelessTestBase {

    @Before
    public void setUp() {
        setupPluginData();

        // Mock the PluginData annotation global behavior
        when(mockPluginDataPath.resolve("global.data")).thenReturn(realPluginDataPath);
    }

    protected void initTest() {
        super.initTest();

        chatGptPromptStateless.setCommentEvent(true);
    }

    private void setupCommandComment(String command) throws RestApiException {
        String commentJson = renderTemplate(
                readTestFile("__files/commands/commandCommentTemplate.json"),
                Map.of("command", command)
        );
        Map<String, List<CommentInfo>> comments = readContentToType(commentJson, COMMENTS_GERRIT_TYPE);
        mockGerritChangeCommentsApiCall(comments);
    }

    private void enableMessageDebugging() {
        when(config.getEnableMessageDebugging()).thenReturn(true);
    }

    private PluginDataHandler getChangeDataHandler() {
        Path realChangeDataPath = tempFolder.getRoot().toPath().resolve(ChatGptTestBase.CHANGE_ID + ".data");
        when(mockPluginDataPath.resolve(ChatGptTestBase.CHANGE_ID + ".data")).thenReturn(realChangeDataPath);
        PluginDataHandlerProvider provider = new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
        PluginDataHandler changeHandler = provider.getChangeScope();
        when(pluginDataHandlerProvider.getChangeScope()).thenReturn(changeHandler);

        return changeHandler;
    }

    @Test
    public void commandMessage() throws RestApiException {
        String message = "is it OK to use \"and/or\"?";
        setupCommandComment("/message " + message);
        mockChatCompletion("chatGptResponseRequestStateless.json");

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        testRequestSent();
        String userPrompt = getUserPrompt();
        Assert.assertTrue(userPrompt.contains(backslashDoubleQuotes(message)));
    }

    @Test
    public void commandReview() throws RestApiException {
        when(globalConfig.getBoolean(Mockito.eq("enabledVoting"), Mockito.anyBoolean()))
                .thenReturn(true);

        setupCommandComment("/review");
        mockChatCompletion("chatGptResponseReview.json");

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        ArgumentCaptor<ReviewInput> captor = testRequestSent();

        Gson gson = OutputFormat.JSON_COMPACT.newGson();
        Assert.assertEquals(gson.toJson(gerritPatchSetReview), gson.toJson(captor.getAllValues().get(0)));
    }

    @Test
    public void commandConfigure() throws Exception {
        String dynamicKey = "gptModel";
        String dynamicValue = "DUMMY_MODEL";
        setupCommandComment(String.format("/configure --%s=%s", dynamicKey, dynamicValue));
        enableMessageDebugging();
        PluginDataHandler changeHandler = getChangeDataHandler();

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        String dynamicChanges = changeHandler.getValue(KEY_DYNAMIC_CONFIG);
        String expectedChanges = getGson().toJson(Map.of(dynamicKey, dynamicValue));
        Assert.assertEquals(expectedChanges, dynamicChanges);
    }

    @Test
    public void commandAddDirective() throws Exception {
        List<String> directives = List.of("DUMMY DIRECTIVE");
        setupCommandComment(String.format("/directives %s", directives.get(0)));
        enableMessageDebugging();
        PluginDataHandler changeHandler = getChangeDataHandler();

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        String dynamicChanges = changeHandler.getValue(KEY_DYNAMIC_CONFIG);
        String expectedChanges = getGson().toJson(Map.of(KEY_DIRECTIVES, getGson().toJson(directives)));
        Assert.assertEquals(expectedChanges, dynamicChanges);
    }

    @Test
    public void commandDumpStoredData() throws Exception {
        setupCommandComment("/dump_stored_data");
        enableMessageDebugging();

        PluginDataHandlerProvider provider = new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
        PluginDataHandler globalHandler = provider.getGlobalScope();
        when(pluginDataHandlerProvider.getGlobalScope()).thenReturn(globalHandler);
        PluginDataHandler projectHandler = provider.getProjectScope();
        when(pluginDataHandlerProvider.getProjectScope()).thenReturn(projectHandler);

        globalHandler.setValue("configKey1", "configValue1");
        globalHandler.setValue("configKey2", "{\"configSubKey\": \"configSubValue\"}");

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        // The dump order may vary, so the contents are compared in sorted form.
        String systemMessage = sortTextLines(readTestFile("__files/commands/dumpStoredDataSystemMessage.txt"));
        Assert.assertEquals(systemMessage, sortTextLines(changeSetData.getReviewSystemMessage()));
    }

    @Test
    public void commandDumpConfig() throws Exception {
        setupCommandComment("/dump_config");
        enableMessageDebugging();

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        String systemMessage = readTestFile("__files/commands/dumpConfig.txt");
        Assert.assertEquals(systemMessage, changeSetData.getReviewSystemMessage());
    }

    @Test
    public void commandUnknown() throws Exception {
        String command = "/UNKNOWN";
        setupCommandComment(command);
        mockChatCompletion("chatGptResponseRequestStateless.json");

        handleEventBasedOnType(EventHandlerTask.SupportedEvents.COMMENT_ADDED);

        String systemMessage = String.format(localizer.getText("message.command.unknown"), "@" + GERRIT_GPT_USERNAME +
                " " + command);
        Assert.assertEquals(systemMessage, changeSetData.getReviewSystemMessage());
    }
}
