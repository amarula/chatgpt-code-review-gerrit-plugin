package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands.ClientCommandParser;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;

@Slf4j
public class ClientMessageParser extends ClientMessageBase {
    private final ClientCommandParser clientCommandParser;

    public ClientMessageParser(
            Configuration config,
            ChangeSetData changeSetData,
            PluginDataHandlerProvider pluginDataHandlerProvider,
            Localizer localizer
    ) {
        super(config);
        clientCommandParser = new ClientCommandParser(config, changeSetData, pluginDataHandlerProvider, localizer);
        log.debug("ClientMessageParser initialized with bot mention pattern: {}", botMentionPattern);
    }

    public boolean isBotAddressed(String message) {
        log.debug("Checking if message addresses the bot: {}", message);
        Matcher userMatcher = botMentionPattern.matcher(message);
        if (!userMatcher.find()) {
            log.debug("Skipping action since the comment does not mention the ChatGPT bot." +
                            " Expected bot name in comment: {}, Actual comment text: {}",
                    config.getGerritUserName(), message);
            return false;
        }
        return true;
    }

    public boolean parseCommands(String comment) {
        log.debug("Parsing commands from comment: {}", comment);
        return clientCommandParser.parseCommands(comment);
    }
}
