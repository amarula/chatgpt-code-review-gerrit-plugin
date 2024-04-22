package com.googlesource.gerrit.plugins.chatgpt.config;

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountManager;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.Accounts;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Singleton
@Slf4j
public class ConfigCreator {

    private final String pluginName;

    private final AccountCache accountCache;
    private final PluginConfigFactory configFactory;

    @Inject
    ConfigCreator(@PluginName String pluginName, AccountCache accountCache, PluginConfigFactory configFactory) {
        this.pluginName = pluginName;
        this.configFactory = configFactory;
        this.accountCache = accountCache;
    }

    public Configuration createConfig(Project.NameKey projectName)
            throws NoSuchProjectException {
        PluginConfig globalConfig = configFactory.getFromGerritConfig(pluginName);
        log.debug("These configuration items have been set in the global configuration: {}", globalConfig.getNames());
        PluginConfig projectConfig = configFactory.getFromProjectConfig(projectName, pluginName);
        log.debug("These configuration items have been set in the project configuration: {}", projectConfig.getNames());
        return new Configuration(globalConfig, projectConfig, getGerritUserEmail(globalConfig));
    }

    private String getGerritUserEmail(PluginConfig globalConfig) {
        String gptUser = globalConfig.getString(Configuration.KEY_GERRIT_USERNAME);
        Optional<AccountState> gptAccount = accountCache.getByUsername(gptUser);
        return gptAccount.map(a -> a.account().preferredEmail()).orElse("");
    }
}
