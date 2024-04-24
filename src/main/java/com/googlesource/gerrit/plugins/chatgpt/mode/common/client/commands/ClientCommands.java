package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.commands;

import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt.Directives;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class ClientCommands {
    private enum COMMAND_SET {
        REVIEW,
        REVIEW_LAST,
        DIRECTIVE
    }
    private enum OPTION_SET {
        FILTER,
        DEBUG
    }

    private static final Map<String, COMMAND_SET> COMMAND_MAP = Map.of(
            "review", COMMAND_SET.REVIEW,
            "review_last", COMMAND_SET.REVIEW_LAST,
            "directive", COMMAND_SET.DIRECTIVE
    );
    private static final Map<String, OPTION_SET> OPTION_MAP = Map.of(
            "filter", OPTION_SET.FILTER,
            "debug", OPTION_SET.DEBUG
    );
    private static final List<COMMAND_SET> REVIEW_COMMANDS = new ArrayList<>(List.of(
            COMMAND_SET.REVIEW,
            COMMAND_SET.REVIEW_LAST
    ));
    private static final List<COMMAND_SET> HISTORY_COMMANDS = new ArrayList<>(List.of(
            COMMAND_SET.DIRECTIVE
    ));
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/(" + String.join("|",
            COMMAND_MAP.keySet()) + ")\\b((?:\\s+--\\w+(?:=\\w+)?)+)?");
    private static final Pattern OPTIONS_PATTERN = Pattern.compile("--(\\w+)(?:=(\\w+))?");

    private final ChangeSetData changeSetData;
    @Getter
    private final Directives directives;
    @Getter
    private boolean containingHistoryCommand;

    public ClientCommands(ChangeSetData changeSetData, GerritChange change) {
        this.changeSetData = changeSetData;
        directives = new Directives(changeSetData);
        containingHistoryCommand = false;
    }

    public boolean parseCommands(String comment, boolean isNotHistory) {
        boolean commandFound = false;
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        while (reviewCommandMatcher.find()) {
            parseCommand(comment, reviewCommandMatcher.group(1), isNotHistory);
            if (reviewCommandMatcher.group(2) != null) {
                parseOption(reviewCommandMatcher, isNotHistory);
            }
            commandFound = true;
        }
        return commandFound;
    }

    public String parseRemoveCommands(String comment) {
        if (parseCommands(comment, false)) {
            return removeCommands(comment);
        }
        return comment;
    }

    private String removeCommands(String comment) {
        Matcher reviewCommandMatcher = COMMAND_PATTERN.matcher(comment);
        return reviewCommandMatcher.replaceAll("");
    }

    private void parseCommand(String comment, String commandString, boolean isNotHistory) {
        COMMAND_SET command = COMMAND_MAP.get(commandString);
        if (isNotHistory && REVIEW_COMMANDS.contains(command)) {
            changeSetData.setForcedReview(true);
            if (command == COMMAND_SET.REVIEW_LAST) {
                log.info("Forced review command applied to the last Patch Set");
                changeSetData.setForcedReviewLastPatchSet(true);
            }
            else {
                log.info("Forced review command applied to the entire Change Set");
            }
        }
        if (HISTORY_COMMANDS.contains(command)) {
            containingHistoryCommand = true;
            directives.addDirective(removeCommands(comment));
        }
    }

    private void parseOption(Matcher reviewCommandMatcher, boolean isNotHistory) {
        COMMAND_SET command = COMMAND_MAP.get(reviewCommandMatcher.group(1));
        Matcher reviewOptionsMatcher = OPTIONS_PATTERN.matcher(reviewCommandMatcher.group(2));
        while (reviewOptionsMatcher.find()) {
            OPTION_SET option = OPTION_MAP.get(reviewOptionsMatcher.group(1));
            if (isNotHistory && REVIEW_COMMANDS.contains(command)) {
                switch (option) {
                    case FILTER:
                        boolean value = Boolean.parseBoolean(reviewOptionsMatcher.group(2));
                        log.info("Option 'replyFilterEnabled' set to {}", value);
                        changeSetData.setReplyFilterEnabled(value);
                        break;
                    case DEBUG:
                        log.info("Response Mode set to Debug");
                        changeSetData.setDebugMode(true);
                        changeSetData.setReplyFilterEnabled(false);
                        break;
                }
            }
        }
    }

}
