package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.language.java;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.CallableLocatorJVM;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class CallableLocator extends CallableLocatorJVM implements IEntityLocator {
    private static final String JAVA_MODULE_EXTENSION = ".java";

    public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config, change, gitRepoFiles);
        log.debug("Initializing CallableLocator for Java projects");
        languageModuleExtension = JAVA_MODULE_EXTENSION;
        importPattern = Pattern.compile(
                String.format("^import\\s+(?:static\\s+)?(%s)", DOT_NOTATION_REGEX),
                Pattern.MULTILINE
        );
    }

    @Override
    protected String getFunctionRegex(String functionName) {
        return "^\\s*(?:@\\w+(?:\\(.*?\\))?\\s*)*" +  // Optional annotations
                "(?:(?:public|protected|private|static|final|abstract|synchronized|native|strictfp)\\s+)*" +  // Optional modifiers
                "(?:<[^>]+>\\s*)?" +  // Optional type parameters
                "\\S+\\s+" +  // Return type
                Pattern.quote(functionName) +  // Method name
                "\\s*\\(.*?\\)" +  // Parameters
                "(?:\\s*throws\\s+[^\\{;]+)?";  // Optional throws clause
    }
}
