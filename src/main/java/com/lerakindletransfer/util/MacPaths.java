package com.lerakindletransfer.util;

import java.nio.file.Path;

public final class MacPaths {
    private static final String APP_NAME = "Lera Kindle Transfer";
    private static final String KEY_FILE_NAME = "koreader_ecdsa";

    private MacPaths() {
    }

    public static Path configDirectory() {
        Path home = Path.of(System.getProperty("user.home"));
        if (isMac()) {
            return home.resolve("Library").resolve("Application Support").resolve(APP_NAME);
        }
        return home.resolve(".config").resolve(APP_NAME);
    }

    public static Path configFile() {
        return configDirectory().resolve("config.json");
    }

    public static Path keysDirectory() {
        return configDirectory().resolve("keys");
    }

    public static Path privateKeyFile() {
        return keysDirectory().resolve(KEY_FILE_NAME);
    }

    public static Path publicKeyFile() {
        return keysDirectory().resolve(KEY_FILE_NAME + ".pub");
    }

    public static Path logsDirectory() {
        Path home = Path.of(System.getProperty("user.home"));
        if (isMac()) {
            return home.resolve("Library").resolve("Logs").resolve(APP_NAME);
        }
        return configDirectory().resolve("logs");
    }

    public static Path cacheDirectory() {
        Path home = Path.of(System.getProperty("user.home"));
        if (isMac()) {
            return home.resolve("Library").resolve("Caches").resolve(APP_NAME);
        }
        return configDirectory().resolve("cache");
    }

    public static Path logFile() {
        return logsDirectory().resolve("app.log");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
