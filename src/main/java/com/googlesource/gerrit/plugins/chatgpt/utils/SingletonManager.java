package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.util.HashMap;
import java.util.Map;

public class SingletonManager {
    private static final Map<String, Object> instances = new HashMap<>();

    public static synchronized <T> T getInstance(Class<T> clazz, String id, Object... constructorArgs) {
        String key = getInstanceKey(clazz, id);
        if (!instances.containsKey(key)) {
            try {
                // Use reflection to invoke constructor with arguments
                T instance;
                if (constructorArgs == null || constructorArgs.length == 0) {
                    instance = clazz.getDeclaredConstructor().newInstance();
                } else {
                    Class<?>[] argClasses = new Class[constructorArgs.length];
                    for (int i = 0; i < constructorArgs.length; i++) {
                        argClasses[i] = constructorArgs[i].getClass();
                    }
                    instance = clazz.getDeclaredConstructor(argClasses).newInstance(constructorArgs);
                }
                instances.put(key, instance);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Error creating instance for class: " + clazz.getName(), e);
            }
        }
        return clazz.cast(instances.get(key));
    }

    public static synchronized void removeInstance(Class<?> clazz, String id) {
        instances.remove(getInstanceKey(clazz, id));
    }

    private static String getInstanceKey(Class<?> clazz, String id) {
        return clazz.getName() + ":" + id;
    }

}
