package com.googlesource.gerrit.plugins.chatgpt.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class JsonTextUtils extends TextUtils {
    private static final Pattern JSON_DELIMITED = Pattern.compile("^.*?" + CODE_DELIMITER + "json\\s*(.*)\\s*" +
                    CODE_DELIMITER + ".*$", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("^\\{.*\\}$", Pattern.DOTALL);

    public static String unwrapJsonCode(String text) {
        return JSON_DELIMITED.matcher(text).replaceAll("$1");
    }

    public static boolean isJsonString(String text) {
        return JSON_OBJECT.matcher(text).matches() || JSON_DELIMITED.matcher(text).matches();
    }
}
