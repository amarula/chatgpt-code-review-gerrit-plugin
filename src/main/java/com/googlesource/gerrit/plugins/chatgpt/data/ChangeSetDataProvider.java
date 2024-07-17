package com.googlesource.gerrit.plugins.chatgpt.data;

import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ChangeSetDataProvider implements Provider<ChangeSetData> {
    private final int gptAccountId;
    private final Configuration config;

    @Inject
    ChangeSetDataProvider(Configuration config, AccountCache accountCache) {
        this.config = config;
        Optional<AccountState> accountState = accountCache.getByUsername(config.getGerritUserName());
        if (accountState.isPresent()) {
            gptAccountId = accountState.get().account().id().get();
            log.debug("GPT account ID set to {}", gptAccountId);
        }
        else {
            log.error("Failed to retrieve GPT account ID for username {}", config.getGerritUserName());
            throw new IllegalStateException("GPT account not found for given username");
        }
    }

    @Override
    public ChangeSetData get() {
        log.debug("Providing ChangeSetData with accountId: {}, minScore: {}, maxScore: {}",
                gptAccountId, config.getVotingMinScore(), config.getVotingMaxScore());
        return new ChangeSetData(gptAccountId, config.getVotingMinScore(), config.getVotingMaxScore());
    }
}
