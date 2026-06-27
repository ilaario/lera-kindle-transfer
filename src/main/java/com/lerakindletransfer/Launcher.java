package com.lerakindletransfer;

import com.lerakindletransfer.util.MacPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        configureJavaFxCache();
        MainApp.main(args);
    }

    private static void configureJavaFxCache() {
        if (!System.getProperty("javafx.cachedir", "").isBlank()) {
            return;
        }
        Path cacheDirectory = writableCacheDirectory(MacPaths.cacheDirectory().resolve("openjfx"));
        if (cacheDirectory == null) {
            cacheDirectory = writableCacheDirectory(Path.of(System.getProperty("java.io.tmpdir"))
                    .resolve("Lera Kindle Transfer")
                    .resolve("openjfx"));
        }
        if (cacheDirectory != null) {
            System.setProperty("javafx.cachedir", cacheDirectory.toString());
        }
    }

    private static Path writableCacheDirectory(Path cacheDirectory) {
        try {
            Files.createDirectories(cacheDirectory);
            return cacheDirectory;
        } catch (IOException ignored) {
            return null;
        }
    }
}
