package com.googlesource.gerrit.plugins.chatgpt.config;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Change;
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
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerBaseProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.googlesource.gerrit.plugins.chatgpt.config.DynamicConfigManager.KEY_DYNAMIC_CONFIG;

@Singleton
@Slf4j
public class ConfigCreator {
    private final String pluginName;

    private final AccountCache accountCache;
    private final PluginConfigFactory configFactory;

    private final OneOffRequestContext context;
    private final GerritApi gerritApi;
    private final PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider;

    @Inject
    ConfigCreator(
            @PluginName String pluginName,
            AccountCache accountCache,
            PluginConfigFactory configFactory,
            OneOffRequestContext context,
            GerritApi gerritApi,
            PluginDataHandlerBaseProvider pluginDataHandlerBaseProvider
    ) {
        this.pluginName = pluginName;
        this.accountCache = accountCache;
        this.configFactory = configFactory;
        this.context = context;
        this.gerritApi = gerritApi;
        this.pluginDataHandlerBaseProvider = pluginDataHandlerBaseProvider;
        log.debug("ConfigCreator initialized for plugin: {}", pluginName);
    }

    public Configuration createConfig(Project.NameKey projectName, Change.Key changeKey) throws NoSuchProjectException {
        log.debug("Creating configuration for project: {} and change: {}", projectName, changeKey);
        PluginConfig globalConfig = configFactory.getFromGerritConfig(pluginName);
        log.debug("Global configuration loaded with items: {}", globalConfig.getNames());
        PluginConfig projectConfig = configFactory.getFromProjectConfig(projectName, pluginName);
        log.debug("Project configuration loaded with items: {}", projectConfig.getNames());
        // `PluginDataHandlerProvider` cannot be injected because `GerritChange` is not initialized at this stage:
        // instead of using `PluginDataHandlerProvider.getChangeScope`, `PluginDataHandlerBaseProvider.get` is employed
        Map<String, String> dynamicConfig = pluginDataHandlerBaseProvider.get(changeKey.toString())
                .getJsonValue(KEY_DYNAMIC_CONFIG, String.class);
        if (dynamicConfig != null && !dynamicConfig.isEmpty()) {
            log.info("DynamicConfig found for change '{}': {}", changeKey, dynamicConfig);
            projectConfig = updateDynamicConfig(projectConfig, pluginName, dynamicConfig);
        }
        Optional<AccountState> gptAccount = getAccount(globalConfig);
        String email = gptAccount.map(a -> a.account().preferredEmail()).orElse("");
        Account.Id accountId =
            gptAccount
                .map(a -> a.account().id())
                .orElseThrow(
                    () ->
                        new RuntimeException(
                            String.format(
                                "Given account %s doesn't exist",
                                globalConfig.getString(Configuration.KEY_GERRIT_USERNAME))));
        return new Configuration(context, gerritApi, globalConfig, projectConfig, email, accountId);
    }

    private Optional<AccountState> getAccount(PluginConfig globalConfig) {
        String gptUser = globalConfig.getString(Configuration.KEY_GERRIT_USERNAME);
        log.debug("Retrieving account for username: {}", gptUser);
        return accountCache.getByUsername(gptUser);
    }

    private PluginConfig updateDynamicConfig(
            PluginConfig projectConfig,
            String pluginName,
            Map<String, String> dynamicConfig
    ) {
        log.debug("Updating dynamic configuration for plugin: {}", pluginName);
        // Retrieve all current configuration values
        Set<String> keys = projectConfig.getNames();
        Map<String, String> currentConfigValues = new HashMap<>();
        for (String key : keys) {
            currentConfigValues.put(key, projectConfig.getString(key));
        }
        // Merge current config with new values from dynamicConfig
        currentConfigValues.putAll(dynamicConfig);
        PluginConfig.Update configUpdater = getProjectUpdate(pluginName, currentConfigValues);

        return configUpdater.asPluginConfig();
    }

    private PluginConfig.@NonNull Update getProjectUpdate(String pluginName, Map<String, String> currentConfigValues) {
        log.debug("Applying merged configuration for plugin: {}", pluginName);
        PluginConfig.Update configUpdater = new PluginConfig.Update(
                pluginName,
                new Config(),
                Optional.empty()
        );
        // Set all merged values
        for (Map.Entry<String, String> entry : currentConfigValues.entrySet()) {
            configUpdater.setString(entry.getKey(), entry.getValue());
        }
        return configUpdater;
    }
}
