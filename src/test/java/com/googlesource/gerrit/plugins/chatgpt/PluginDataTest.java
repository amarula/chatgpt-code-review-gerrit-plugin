package com.googlesource.gerrit.plugins.chatgpt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;

import com.googlesource.gerrit.plugins.chatgpt.data.PluginDataHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PluginDataTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private Path mockPluginDataPath;

    private Path realPluginDataPath;

    @Before
    public void setUp() {
        // Setup temporary folder for tests
        realPluginDataPath = tempFolder.getRoot().toPath().resolve("plugin.config");

        // Mock the PluginData annotation behavior
        when(mockPluginDataPath.resolve("plugin.config")).thenReturn(realPluginDataPath);
    }

    @Test
    public void testValueSetAndGet() {
        PluginDataHandler handler = new PluginDataHandler(mockPluginDataPath);

        String key = "testKey";
        String value = "testValue";

        // Test set value
        handler.setValue(key, value);

        // Test get value
        assertEquals("The value retrieved should match the value set.", value, handler.getValue(key));
    }

    @Test
    public void testRemoveValue() {
        PluginDataHandler handler = new PluginDataHandler(mockPluginDataPath);

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

        new PluginDataHandler(mockPluginDataPath);

        // The constructor should create the file if it doesn't exist
        assertTrue("The config file should exist after initializing the handler.", Files.exists(realPluginDataPath));
    }

}
