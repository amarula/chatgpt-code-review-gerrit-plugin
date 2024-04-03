package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) {
        try {
            // Attempt to load the class using the default class loader
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return loadLocalClass(name);
        }
    }

    private Class<?> loadLocalClass(String name) {
        try {
            byte[] b = loadClassData(name);
            return defineClass(name, b, 0, b.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load local class " + name, e);
        }
    }

    private byte[] loadClassData(String name) throws IOException {
        ByteArrayOutputStream byteSt;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(
                name.replace('.', '/') + ".class")) {
            byteSt = new ByteArrayOutputStream();
            int len;
            while ((len = is.read()) != -1) {
                byteSt.write(len);
            }
        }
        catch (NullPointerException e) {
            throw new RuntimeException("Failed to load class data for class " + name, e);
        }
        return byteSt.toByteArray();
    }

}
