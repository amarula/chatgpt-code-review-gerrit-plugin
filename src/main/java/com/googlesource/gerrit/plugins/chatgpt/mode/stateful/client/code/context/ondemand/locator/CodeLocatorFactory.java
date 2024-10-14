package com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.code.context.ondemand.locator;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.errors.exceptions.CodeContextOnDemandLocatorException;
import com.googlesource.gerrit.plugins.chatgpt.interfaces.mode.stateful.client.code.context.ondemand.IEntityLocator;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptGetContextItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.stateful.client.api.git.GitRepoFiles;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils.getExtension;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.convertSnakeToPascalCase;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithDot;

@Slf4j
public class CodeLocatorFactory {

    public static final String LANGUAGE_PACKAGE = "language";

    public enum EntityCategory {
        Callable, Data, Type
    }

    private static final Map<String, String> MAP_EXTENSION = Map.of(
            "py", "python",
            "java", "java",
            "c", "c",
            "h", "c"
    );

    private static List<String> classNameComponents;

    public static IEntityLocator getEntityLocator(
            ChatGptGetContextItem chatGptGetContextItem,
            Configuration config,
            GerritChange change,
            GitRepoFiles gitRepoFiles
    ) throws CodeContextOnDemandLocatorException {
        log.debug("Getting Entity Locator for context item {}", chatGptGetContextItem);
        IEntityLocator entityLocator;
        classNameComponents = new ArrayList<>(List.of(
                CodeLocatorFactory.class.getPackage().getName(),
                LANGUAGE_PACKAGE
        ));
        addProgrammingLanguage(chatGptGetContextItem);
        addLocatorTypeClass(chatGptGetContextItem);
        String className = joinWithDot(classNameComponents);
        log.debug("Entity locator class name found: {}", className);
        try {
            @SuppressWarnings("unchecked")
            Class<IEntityLocator> clazz = (Class<IEntityLocator>) Class.forName(className);
            entityLocator = clazz
                    .getDeclaredConstructor(Configuration.class, GerritChange.class, GitRepoFiles.class)
                    .newInstance(config, change, gitRepoFiles);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            log.warn("Entity locator `{}` class not found for Get-Context Item: {}", className, chatGptGetContextItem);
            throw new CodeContextOnDemandLocatorException(e);
        }
        return entityLocator;
    }


    private static void addProgrammingLanguage(ChatGptGetContextItem chatGptGetContextItem)
            throws CodeContextOnDemandLocatorException {
        String language = getCodeLocatorLanguage(chatGptGetContextItem);
        if (language == null) {
            log.warn("No language supported for file {}", chatGptGetContextItem.getFilename());
            throw new CodeContextOnDemandLocatorException();
        }
        classNameComponents.add(language);
    }

    private static void addLocatorTypeClass(ChatGptGetContextItem chatGptGetContextItem)
            throws CodeContextOnDemandLocatorException {
        EntityCategory entityCategory;
        try {
            entityCategory = EntityCategory.valueOf(
                    convertSnakeToPascalCase(chatGptGetContextItem.getEntityCategory())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity category: {}", chatGptGetContextItem.getEntityCategory());
            throw new CodeContextOnDemandLocatorException();
        }
        classNameComponents.add(entityCategory + "Locator");
    }

    private static String getCodeLocatorLanguage(ChatGptGetContextItem chatGptGetContextItem)
            throws CodeContextOnDemandLocatorException {
        String fileExtension;
        try {
            fileExtension = getExtension(chatGptGetContextItem.getFilename());
        } catch (Exception e) {
            throw new CodeContextOnDemandLocatorException();
        }
        return MAP_EXTENSION.get(fileExtension);
    }
}
