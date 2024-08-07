package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.chatgpt;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.OpenAiConnectionFailException;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.UriResourceLocatorStateful;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.api.chatgpt.ChatGptResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.URI;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class ChatGptThread {
    public static final String KEY_THREAD_ID = "threadId";

    private final ChatGptHttpClient httpClient = new ChatGptHttpClient();
    private final Configuration config;
    private final ChangeSetData changeSetData;
    private final PluginDataHandler changeDataHandler;

    public ChatGptThread(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider
    ) {
        this.config = config;
        this.changeSetData = changeSetData;
        this.changeDataHandler = pluginDataHandlerProvider.getChangeScope();
    }

    public String createThread() throws OpenAiConnectionFailException {
        String threadId = changeDataHandler.getValue(KEY_THREAD_ID);
        if (threadId == null || !changeSetData.getForcedReview() && !changeSetData.getForcedStagedReview()) {
            Request request = createThreadRequest();
            log.debug("ChatGPT Create Thread request: {}", request);

            ChatGptResponse threadResponse = getGson().fromJson(httpClient.execute(request), ChatGptResponse.class);
            threadId = threadResponse.getId();
            if (threadId != null) {
                log.info("Thread created: {}", threadResponse);
                changeDataHandler.setValue(KEY_THREAD_ID, threadId);
            }
            else {
                log.error("Failed to create thread. Response: {}", threadResponse);
            }
        }
        else {
            log.info("Existing thread found for the Change Set. Thread ID: {}", threadId);
        }
        return threadId;
    }

    private Request createThreadRequest() {
        URI uri = URI.create(config.getGptDomain() + UriResourceLocatorStateful.threadsUri());
        log.debug("ChatGPT Create Thread request URI: {}", uri);

        return httpClient.createRequestFromJson(uri.toString(), config.getGptToken(), new Object());
    }
}
