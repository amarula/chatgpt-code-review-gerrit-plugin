package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.language.kotlin;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.CallableLocatorJVM;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class CallableLocator extends CallableLocatorJVM implements IEntityLocator {
    private static final String KOTLIN_MODULE_EXTENSION = ".kt";
    private static final String ALTERNATIVE_BASE_PATH = "app/src/main/kotlin/";
    private static final String NON_MODIFIABLE_BASE_PATH = "app/src/";

    public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config, change, gitRepoFiles);
        log.debug("Initializing CallableLocator for Kotlin projects");
        languageModuleExtension = KOTLIN_MODULE_EXTENSION;
        importPattern = Pattern.compile(
                String.format("^import\\s+(%s)", DOT_NOTATION_REGEX),
                Pattern.MULTILINE
        );
    }

    @Override
    protected String getFunctionRegex(String functionName) {
        return "^\\s*(?:@\\w+(?:\\(.*?\\))?\\s*)*" +  // Optional annotations
                "(?:\\w+\\s+)*" +                      // Optional modifiers
                "fun\\s+" +                            // 'fun' keyword
                Pattern.quote(functionName) +          // Function name
                "\\s*\\(.*?\\)" +                      // Parameters
                "(?:\\s*:\\s*\\S+)?";                  // Optional return type
    }

    @Override
    protected void parseImportStatements(String content) {
        parseDirectImportStatements(content, importModules);
        importModules.addAll(importModules.stream()
                .filter(s -> !s.startsWith(NON_MODIFIABLE_BASE_PATH))
                .map(s -> ALTERNATIVE_BASE_PATH + s)
                .toList()
        );
        retrievePackageModules();
    }
}
