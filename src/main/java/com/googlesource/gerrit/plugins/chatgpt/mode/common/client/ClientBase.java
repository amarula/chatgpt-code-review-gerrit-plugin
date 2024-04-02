package com.googlesource.gerrit.plugins.chatgpt.mode.common.client;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;

public abstract class ClientBase {
    protected Configuration config;

    public ClientBase(Configuration config) {
        this.config = config;
    }

}
