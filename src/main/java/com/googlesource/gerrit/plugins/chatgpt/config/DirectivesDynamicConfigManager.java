package com.googlesource.gerrit.plugins.chatgpt.config;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.exceptions.DynamicDirectivesModifyException;
import lombok.Getter;

import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.JsonTextUtils.jsonArrayToList;

public class DirectivesDynamicConfigManager extends DynamicConfigManager {
    @Getter
    private final List<String> directives;

    public DirectivesDynamicConfigManager(PluginDataHandlerProvider pluginDataHandlerProvider) {
        super(pluginDataHandlerProvider);
        directives = jsonArrayToList(getConfig(Configuration.KEY_DIRECTIVES));
    }

    public void addDirective(String directive) {
        directives.add(directive);
        updateConfiguration();
    }

    public void removeDirective(String directiveIndex) throws DynamicDirectivesModifyException {
        try {
            int index = Integer.parseInt(directiveIndex) - 1;
            directives.remove(index);
        }
        catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new DynamicDirectivesModifyException(e.getMessage());
        }
        updateConfiguration();
    }

    public void resetDirectives() {
        directives.clear();
        setConfig(Configuration.KEY_DIRECTIVES, "");
        updateConfiguration(true, true);
    }

    private void updateConfiguration() {
        setConfig(Configuration.KEY_DIRECTIVES, getGson().toJson(directives));
        updateConfiguration(false, true);
    }
}
