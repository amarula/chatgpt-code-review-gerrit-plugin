package com.googlesource.gerrit.plugins.chatgpt.config;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.util.OneOffRequestContext;
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

    private final OneOffRequestContext context;
    private final GerritApi gerritApi;

    @Inject
    ConfigCreator(@PluginName String pluginName, AccountCache accountCache, PluginConfigFactory configFactory, OneOffRequestContext context, GerritApi gerritApi) {
        this.pluginName = pluginName;
        this.accountCache = accountCache;
        this.configFactory = configFactory;
        this.context = context;
        this.gerritApi = gerritApi;
    }

    public Configuration createConfig(Project.NameKey projectName)
            throws NoSuchProjectException {
        PluginConfig globalConfig = configFactory.getFromGerritConfig(pluginName);
        log.debug("These configuration items have been set in the global configuration: {}", globalConfig.getNames());
        PluginConfig projectConfig = configFactory.getFromProjectConfig(projectName, pluginName);
        log.debug("These configuration items have been set in the project configuration: {}", projectConfig.getNames());
        Optional<AccountState> gptAccount = getAccount(globalConfig);
        String email = gptAccount.map(a -> a.account().preferredEmail()).orElse("");
        Account.Id accountId = gptAccount.map(a -> a.account().id()).orElseGet(() -> Account.id(1000000));
        return new Configuration(context, gerritApi, globalConfig, projectConfig, email, accountId);
    }

    private Optional<AccountState> getAccount(PluginConfig globalConfig) {
        String gptUser = globalConfig.getString(Configuration.KEY_GERRIT_USERNAME);
        return accountCache.getByUsername(gptUser);
    }
}
