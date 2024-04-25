package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.ClientBase;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;


@Slf4j
public abstract class GerritClientBase extends ClientBase {
    @Getter
    protected HashMap<String, FileDiffProcessed> fileDiffsProcessed = new HashMap<>();

    public GerritClientBase(Configuration config) {
        super(config);
    }
}
