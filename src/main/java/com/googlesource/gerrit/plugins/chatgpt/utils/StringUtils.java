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

    public static String concatenate(List<String> components) {
        return String.join("", components);
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
