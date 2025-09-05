package com.googlesource.gerrit.plugins.chatgpt;

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.server.events.Event;
import com.google.inject.Provides;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.listener.GerritEventContextModule;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TestGerritEventContextModule extends GerritEventContextModule {

  public TestGerritEventContextModule(Configuration config, Event event) {
    super(config, event);
  }

  @Provides
  @PluginData
  Path providePluginDataPath() {
    return Paths.get(System.getProperty("pluginDataPath", "test-plugin-data"));
  }
}
