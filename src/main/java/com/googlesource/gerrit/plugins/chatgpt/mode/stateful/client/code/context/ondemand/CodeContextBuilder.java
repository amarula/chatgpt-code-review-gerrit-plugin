package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.CodeContextOnDemandLocatorException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextContent;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.model.code.context.ondemand.GetContextOutputItem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.CodeLocatorFactory.getEntityLocator;
import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;

@Slf4j
public class CodeContextBuilder extends ClientBase {
    private static final String CONTEXT_NOT_PROVIDED = "CONTEXT NOT PROVIDED";

    private final GerritChange change;
    private final GitRepoFiles gitRepoFiles;

    public CodeContextBuilder(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config);
        this.change = change;
        this.gitRepoFiles = gitRepoFiles;
    }

    public String buildCodeContext(ChatGptGetContextContent getContextContent) {
        log.debug("Building code context for {}", getContextContent);
        List<GetContextOutputItem> getContextOutput = new ArrayList<>();
        for (ChatGptGetContextItem chatGptGetContextItem : getContextContent.getReplies()) {
            IEntityLocator entityLocator;
            try {
                entityLocator = getEntityLocator(chatGptGetContextItem, config, change, gitRepoFiles);
            } catch (CodeContextOnDemandLocatorException e) {
                continue;
            }
            String definition = entityLocator.findDefinition(chatGptGetContextItem);
            if (definition == null) {
                log.warn("Unable to find definition for `{}`", chatGptGetContextItem.getContextRequiredEntity());
                definition = CONTEXT_NOT_PROVIDED;
            }
            GetContextOutputItem getContextOutputItem = GetContextOutputItem.builder()
                    .requestType(chatGptGetContextItem.getRequestType())
                    .entityCategory(chatGptGetContextItem.getEntityCategory())
                    .contextRequiredEntity(chatGptGetContextItem.getContextRequiredEntity())
                    .definition(definition)
                    .build();
            log.debug("Added code context: {}", getContextOutputItem);
            getContextOutput.add(getContextOutputItem);
        }
        return getGson().toJson(getContextOutput);
    }
}
