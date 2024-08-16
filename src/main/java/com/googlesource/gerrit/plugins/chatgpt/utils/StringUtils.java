package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class StringUtils {
    public static String backslashEachChar(String body) {
        StringBuilder slashedBody = new StringBuilder();

        for (char ch : body.toCharArray()) {
            slashedBody.append("\\\\").append(ch);
        }
        return slashedBody.toString();
    }

    public static String backslashDoubleQuotes(String body) {
        return body.replace("\"", "\\\"");
    }

    public static String doubleBackslashDoubleQuotes(String body) {
        return body.replace("\"", "\\\\\"");
    }

    public static String deSlash(String target, String deSlashChars) {
        return target.replaceAll("\\\\([" + deSlashChars + "])", "$1");
    }

    public static String concatenate(List<String> components) {
        return String.join("", components);
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String convertPascalCaseToWords(String pascalCase) {
        if (pascalCase == null || pascalCase.isEmpty()) {
            return pascalCase;
        }
        return pascalCase.replaceAll("(?<!^)([A-Z])", " $1");
    }

    public static String convertCamelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("(?<!^)([A-Z])", "_$1").toLowerCase();
    }

}
