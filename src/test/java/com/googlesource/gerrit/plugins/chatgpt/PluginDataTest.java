package com.googlesource.gerrit.plugins.chatgpt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.nio.file.Files;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandlerProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PluginDataTest extends TestBase {

  @Before
  public void setUp() {
    setupPluginData();

    // Mock the PluginData annotation global behavior
    when(mockPluginDataPath.resolve("global.data")).thenReturn(realPluginDataPath);
  }

  @Test
  public void testValueSetAndGet() {
    PluginDataHandlerProvider provider =
        new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
    PluginDataHandler globalHandler = provider.getGlobalScope();
    PluginDataHandler projectHandler = provider.getProjectScope();

    String key = "testKey";
    String value = "testValue";

    // Test set value
    globalHandler.setValue(key, value);
    projectHandler.setValue(key, value);

    // Test get value
    assertEquals(
        "The value retrieved should match the value set.", value, globalHandler.getValue(key));
    assertEquals(
        "The value retrieved should match the value set.", value, projectHandler.getValue(key));
  }

  @Test
  public void testRemoveValue() {
    PluginDataHandlerProvider provider =
        new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
    PluginDataHandler handler = provider.getGlobalScope();

    String key = "testKey";
    String value = "testValue";

    // Set a value to ensure it can be removed
    handler.setValue(key, value);
    // Remove the value
    handler.removeValue(key);

    // Verify the value is no longer available
    assertNull("The value should be null after being removed.", handler.getValue(key));
  }

  @Test
  public void testCreateFileOnNonexistent() throws Exception {
    // Ensure the file doesn't exist before creating the handler
    Files.deleteIfExists(realPluginDataPath);

    PluginDataHandlerProvider provider =
        new PluginDataHandlerProvider(mockPluginDataPath, getGerritChange());
    provider.getGlobalScope();

    // The constructor should create the file if it doesn't exist
    assertTrue(
        "The config file should exist after initializing the handler.",
        Files.exists(realPluginDataPath));
  }
}
