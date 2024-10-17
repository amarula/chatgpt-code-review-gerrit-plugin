package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.language.python;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator.CallableLocatorBase;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.ITEM_COMMA_DELIMITED_REGEX;

@Slf4j
public class CallableLocator extends CallableLocatorBase implements IEntityLocator {
    private static final String PYTHON_MODULE_EXTENSION = ".py";

    private final Map<String, String> fromModuleMap = new HashMap<>();

    public CallableLocator(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config, change, gitRepoFiles);
        log.debug("Initializing FunctionLocator");
        languageModuleExtension = PYTHON_MODULE_EXTENSION;
        importPattern = Pattern.compile(
                String.format(
                        "^(?:from\\s+(%1$s)\\s+import\\s+(\\*|\\w+(?:%2$s\\w+)*)|import\\s+(%1$s(?:%2$s%1$s))*)",
                        DOT_NOTATION_REGEX,
                        ITEM_COMMA_DELIMITED_REGEX
                ),
                Pattern.MULTILINE
        );
    }

    @Override
    protected String getFunctionRegex(String functionName) {
        return "^\\s*(?:async\\s+)?def\\s+" + Pattern.quote(functionName) + "\\s*" +
                "(?:\\[[^]]+\\]\\s*)?" +    // Type Parameter List
                "(?:\\([^)]*\\)\\s*)?" +    // Arguments
                "(?:->[^:]+\\s*)?:";        // Return type
    }

    @Override
    protected void parseImportStatements(String content) {
        log.debug("Parsing import statements");
        Matcher importMatcher = importPattern.matcher(content);
        while (importMatcher.find()) {
            String fromModuleGroup = importMatcher.group(1);
            String fromEntitiesGroup = importMatcher.group(2);
            String importModulesGroup = importMatcher.group(3);

            if (fromModuleGroup != null) {
                log.debug("Parsing FROM module: `{}` / IMPORT entities: `{}`", fromModuleGroup, fromEntitiesGroup);
                // The module is included in both `fromModuleMap` and `importModules`. The second case handles
                // `from MODULE import *` statements and component class invocations that use dot notation.
                getGroupStream(fromEntitiesGroup).forEach(entity -> fromModuleMap.put(entity, fromModuleGroup));
                importModules.add(fromModuleGroup);
            }
            if (importModulesGroup != null) {
                log.debug("Parsing IMPORT modules: `{}`", importModulesGroup);
                getGroupStream(importModulesGroup).forEach(importModules::add);
            }
        }
        log.debug("Found fromModuleMap: {}", fromModuleMap);
        log.debug("Found importModules: {}", importModules);
    }

    @Override
    protected String findInImportModules(String functionName) {
        // Check if the function is directly imported via `from MODULE import ENTITY`
        if (fromModuleMap.containsKey(functionName)) {
            String module = fromModuleMap.get(functionName);
            log.debug("Function {} is directly imported from module: {}", functionName, module);
            String result = getFunctionFromModule(functionName, module);
            if (result != null) return result;
        }
        // If not found, proceed to check modules imported via `import MODULE`
        return findInModules(functionName);
    }
}
