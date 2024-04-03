package com.googlesource.gerrit.plugins.chatgpt.mode;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.utils.CustomClassLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ClassUtils.createNewInstanceWithArgs;
import static com.googlesource.gerrit.plugins.chatgpt.utils.ClassUtils.joinUsingDotNotation;
import static com.googlesource.gerrit.plugins.chatgpt.utils.StringUtils.capitalizeFirstLetter;

@Slf4j
public class ModeClassLoader {
    private static final String BASE_NAME = "com.googlesource.gerrit.plugins.chatgpt.mode";

    public static Object getInstance(String className, Configuration config, Object... constructorArgs) {
        CustomClassLoader customClassLoader = new CustomClassLoader();
        String fullClassName = getFullClassName(className, config);
        log.debug("Attempt to load Class '{}'", fullClassName);
        try {
            Class<?> DynamicallyLoadedClass = Class.forName(fullClassName, true, customClassLoader);
            log.debug("Class '{}' loaded", fullClassName);
            return createNewInstanceWithArgs(DynamicallyLoadedClass, constructorArgs);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFullClassName(String className, Configuration config) {
        String mode = config.getGptMode().name();
        return joinUsingDotNotation(new ArrayList<>() {{
            add(BASE_NAME);
            add(mode);
            add(className + capitalizeFirstLetter(mode));
        }});
    }

}
