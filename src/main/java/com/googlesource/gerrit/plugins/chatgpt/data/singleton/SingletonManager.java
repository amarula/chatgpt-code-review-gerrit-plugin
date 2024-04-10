package com.googlesource.gerrit.plugins.chatgpt.data.singleton;

import java.util.HashMap;
import java.util.Map;

import static com.googlesource.gerrit.plugins.chatgpt.utils.ClassUtils.createNewInstanceWithArgs;

public class SingletonManager {
    private static final Map<String, Object> instances = new HashMap<>();

    public static synchronized <T> T getInstance(Class<T> clazz, String key, Object... constructorArgs) {
        if (!instances.containsKey(key)) {
            return createNewInstance(clazz, key, constructorArgs);
        }
        return clazz.cast(instances.get(key));
    }

    public static synchronized <T> T getNewInstance(Class<T> clazz, String key, Object... constructorArgs) {
        return createNewInstance(clazz, key, constructorArgs);
    }

    public static synchronized void removeInstance(Class<?> clazz, String key) {
        instances.remove(key);
    }

    private static synchronized <T> T createNewInstance(Class<T> clazz, String key, Object... constructorArgs) {
        try {
            // Use reflection to invoke constructor with arguments
            T instance;
            if (constructorArgs == null || constructorArgs.length == 0) {
                instance = clazz.getDeclaredConstructor().newInstance();
            } else {
                instance = createNewInstanceWithArgs(clazz, constructorArgs);
            }
            instances.put(key, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error creating instance for class: " + clazz.getName(), e);
        }
    }

}
