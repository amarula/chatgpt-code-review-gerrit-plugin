/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.reviewai;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.nio.file.Files;

import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandler;
import com.googlesource.gerrit.plugins.reviewai.data.PluginDataHandlerProvider;
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
