package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.language.java;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.CallableLocatorBase;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CallableLocator extends CallableLocatorBase implements IEntityLocator {
    private static final String JAVA_MODULE_EXTENSION = ".java";
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            String.format("^import\\s+(?:static\\s+)?(%s)", DOT_NOTATION_REGEX),
            Pattern.MULTILINE
    );

    public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config, change, gitRepoFiles);
        log.debug("Initializing FunctionLocator");
        languageModuleExtension = JAVA_MODULE_EXTENSION;
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

    @Override
    protected String findImportedFunctionDefinition(String functionName, String content) {
        parseImportStatements(content);
        retrievePackageModules();

        return findInModules(functionName);
    }

    private void retrievePackageModules() {
        log.debug("Retrieving modules from current Package");
        List<String> packageModules = codeFileFetcher.getFilesInDir(rootFileDir)
                .stream()
                .map(FileUtils::removeExtension)
                .toList();
        log.debug("Modules retrieved from current Package: {}", packageModules);
        importModules.addAll(packageModules);
    }

    private void parseImportStatements(String content) {
        log.debug("Parsing import statements");
        Matcher importMatcher = IMPORT_PATTERN.matcher(content);
        while (importMatcher.find()) {
            String importModulesGroup = importMatcher.group(1);
            log.debug("Parsing IMPORT module: `{}`", importModulesGroup);
            importModules.add(importModulesGroup);
        }
        log.debug("Found importModules from import statements: {}", importModules);
    }
}
