package com.googlesource.gerrit.plugins.chatgpt.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {
    public static Gson getGson() {
        return new Gson();
    }

    public static Gson getNoEscapedGson() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }

}
