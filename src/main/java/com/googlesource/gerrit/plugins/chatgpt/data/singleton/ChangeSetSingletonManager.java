package com.googlesource.gerrit.plugins.chatgpt.data.singleton;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit.GerritChange;

public class ChangeSetSingletonManager {
    public static synchronized <T> T getNewInstance(Class<T> clazz, GerritChange change, Object... constructorArgs) {
        return SingletonManager.getNewInstance(clazz, getInstanceKey(clazz, change), constructorArgs);
    }

    public static synchronized <T> T getInstance(Class<T> clazz, GerritChange change, Object... constructorArgs) {
        return SingletonManager.getInstance(clazz, getInstanceKey(clazz, change), constructorArgs);
    }

    public static synchronized void removeInstance(Class<?> clazz, GerritChange change) {
        SingletonManager.removeInstance(clazz, getInstanceKey(clazz, change));
    }

    private static String getInstanceKey(Class<?> clazz, GerritChange change) {
        return clazz.getName() + ":" + change.getFullChangeId();
    }

}
