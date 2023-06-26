package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ConfigCreator {

    private final String pluginName;

    private final PluginConfigFactory configFactory;

    @Inject
    ConfigCreator(@PluginName String pluginName, PluginConfigFactory configFactory) {
        this.pluginName = pluginName;
        this.configFactory = configFactory;
    }

    public Configuration createConfig(Project.NameKey projectName)
            throws NoSuchProjectException {
        PluginConfig globalConfig = configFactory.getFromGerritConfig(pluginName);
        log.info("The names in global config: {}", globalConfig.getNames());
        PluginConfig projectConfig = configFactory.getFromProjectConfig(projectName, pluginName);
        log.info("The names in project config: {}", projectConfig.getNames());
        return new Configuration(globalConfig, projectConfig);
    }
}
