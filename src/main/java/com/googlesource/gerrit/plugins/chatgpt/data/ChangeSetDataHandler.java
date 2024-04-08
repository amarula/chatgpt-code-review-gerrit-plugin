package com.googlesource.gerrit.plugins.chatgpt.data;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.ChatGptUserPrompt;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritPermittedVotingRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.utils.SingletonManager;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

@Slf4j
public class ChangeSetDataHandler {

    public static ChangeSetData getNewInstance(Configuration config, GerritChange change, Integer gptAccountId) {
        return SingletonManager.getNewInstance(ChangeSetData.class, change,
                gptAccountId,
                config.getVotingMinScore(),
                config.getVotingMaxScore());
    }

    public static ChangeSetData getInstance(GerritChange change) {
        return SingletonManager.getInstance(ChangeSetData.class, change);
    }

    public static ChangeSetData getInstance(String changeId) {
        return SingletonManager.getInstance(ChangeSetData.class, changeId);
    }

    public static void update(Configuration config, GerritChange change, GerritClient gerritClient) {
        ChangeSetData changeSetData = getInstance(change);
        GerritClientData gerritClientData = gerritClient.getClientData(change);
        ChatGptUserPrompt chatGptUserPrompt = new ChatGptUserPrompt(config, change, gerritClientData);

        changeSetData.setCommentPropertiesSize(gerritClientData.getCommentProperties().size());
        changeSetData.setDirectives(new HashSet<>());
        changeSetData.setGptRequestUserPrompt(chatGptUserPrompt.buildPrompt());
        if (config.isVotingEnabled() && !change.getIsCommentEvent()) {
            GerritPermittedVotingRange permittedVotingRange = gerritClient.getPermittedVotingRange(change);
            if (permittedVotingRange != null) {
                if (permittedVotingRange.getMin() > config.getVotingMinScore()) {
                    log.debug("Minimum ChatGPT voting score set to {}", permittedVotingRange.getMin());
                    changeSetData.setVotingMinScore(permittedVotingRange.getMin());
                }
                if (permittedVotingRange.getMax() < config.getVotingMaxScore()) {
                    log.debug("Maximum ChatGPT voting score set to {}", permittedVotingRange.getMax());
                    changeSetData.setVotingMaxScore(permittedVotingRange.getMax());
                }
            }
        }
    }

    public static void removeInstance(GerritChange change) {
        SingletonManager.removeInstance(ChangeSetData.class, change.getFullChangeId());
    }

}
