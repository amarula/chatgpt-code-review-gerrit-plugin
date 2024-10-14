package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.util.List;

public class ModuleUtils {
    public static final String MODULE_SEPARATOR = ".";

    public static String convertDotNotationToPath(String dotNotated) {
        return dotNotated.replace(MODULE_SEPARATOR.charAt(0), '/');
    }

    public static String getSimpleName(String moduleName) {
        int lastDotIndex = moduleName.lastIndexOf(MODULE_SEPARATOR);
        if (lastDotIndex <= 0 || lastDotIndex == moduleName.length() - 1) {
            return moduleName;
        }
        return moduleName.substring(lastDotIndex + 1);
    }

    public static String joinComponents(List<String> components) {
        return String.join(MODULE_SEPARATOR, components);
    }
}
