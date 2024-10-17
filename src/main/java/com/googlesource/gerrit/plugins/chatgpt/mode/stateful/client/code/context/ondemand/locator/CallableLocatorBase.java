package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.CodeFileFetcher;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ModuleUtils.convertDotNotationToPath;
import static com.googlesource.gerrit.plugins.chatgpt.utils.ModuleUtils.getSimpleName;
import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.getDirName;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.cutString;

@Slf4j
public abstract class CallableLocatorBase extends ClientBase implements IEntityLocator {
    protected static final String DOT_NOTATION_REGEX = "[\\w.]+";
    private static final int LOG_MAX_CONTENT_SIZE = 256;

    protected final List<String> importModules = new ArrayList<>();
    protected final CodeFileFetcher codeFileFetcher;

    protected Pattern importPattern;
    protected String languageModuleExtension;
    protected String rootFileDir;

    private Set<String> visitedFiles;

    public CallableLocatorBase(Configuration config, GerritChange change, GitRepoFiles gitRepoFiles) {
        super(config);
        log.debug("Initializing FunctionLocatorBase");
        codeFileFetcher = new CodeFileFetcher(config, change, gitRepoFiles);
    }

    public String findDefinition(ChatGptGetContextItem chatGptGetContextItem) {
        log.debug("Finding function definition for {}", chatGptGetContextItem);
        visitedFiles = new HashSet<>();
        String filename = chatGptGetContextItem.getFilename();
        String functionName = getSimpleName(chatGptGetContextItem.getContextRequiredEntity());
        rootFileDir = getDirName(filename);
        log.debug("Root file dir: {}", rootFileDir);

        return findFunctionInFile(filename, functionName);
    }

    protected abstract String getFunctionRegex(String functionName);

    protected abstract void parseImportStatements(String content);

    protected abstract String findInImportModules(String functionName);

    protected String getFunctionFromModule(String functionName, String module) {
        String modulePath = convertDotNotationToPath(module) + languageModuleExtension;
        modulePath = modulePath.replaceAll("^(?=/)", rootFileDir);
        log.debug("Module path: {}", modulePath);

        return findFunctionInFile(modulePath, functionName);
    }

    protected Stream<String> getGroupStream(String group) {
        return Arrays.stream(group.split(","))
                .map(String::trim)
                .filter(entity -> !entity.isEmpty());
    }

    protected String findInModules(String functionName) {
        for (String module : importModules) {
            log.debug("Searching for function `{}` in module: {}", functionName, module);
            String result = getFunctionFromModule(functionName, module);
            if (result != null) return result;
        }
        return null;
    }

    private String findImportedFunctionDefinition(String functionName, String content) {
        parseImportStatements(content);

        return findInImportModules(functionName);
    }

    private String findFunctionInFile(String filename, String functionName) {
        log.debug("Finding function {} in file {}", functionName, filename);
        if (visitedFiles.contains(filename)) {
            log.debug("File {} already visited", filename);
            return null;
        }
        visitedFiles.add(filename);

        String content;
        try {
            content = codeFileFetcher.getFileContent(filename);
        } catch (IOException e) {
            log.debug("File `{}` not found in the git repository", filename);
            return null;
        }
        log.debug("File content retrieved for file `{}`:\n{}", filename, cutString(content, LOG_MAX_CONTENT_SIZE));

        // Search the file for the function definition
        Pattern functionPattern = Pattern.compile(getFunctionRegex(functionName), Pattern.MULTILINE);
        Matcher functionMatcher = functionPattern.matcher(content);
        if (functionMatcher.find()) {
            String functionDefinition = functionMatcher.group(0).trim();
            log.debug("Found function definition: {}", functionDefinition);
            return functionDefinition;
        }
        return findImportedFunctionDefinition(functionName, content);
    }
}
