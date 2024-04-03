package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ClassUtils {
    public static synchronized <T> T createNewInstanceWithArgs(Class<T> clazz, Object... constructorArgs) {
        Class<?>[] argClasses = new Class[constructorArgs.length];
        for (int i = 0; i < constructorArgs.length; i++) {
            argClasses[i] = constructorArgs[i].getClass();
        }
        try {
            return clazz.getDeclaredConstructor(argClasses).newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerDynamicClasses(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            assert clazz != null; // This assertion is always true; serves to silence "unused" warnings
        }
    }

    public static String joinUsingDotNotation(List<String> components) {
        return String.join(".", components);
    }

}
