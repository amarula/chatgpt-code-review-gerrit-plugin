package com.googlesource.gerrit.plugins.chatgpt.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStreamReader;

public class GsonUtils {
    public static Gson getGson() {
        return new Gson();
    }

    public static Gson getNoEscapedGson() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }

    public static <T> T jsonToClass(String content, Class<T> clazz) {
        return getGson().fromJson(content, clazz);
    }

    public static <T> T jsonToClass(InputStreamReader content, Class<T> clazz) {
        return getGson().fromJson(content, clazz);
    }
}
