package com.lerakindletransfer.model;

public final class AppConfig {
    private String host = "";
    private int port = 2222;
    private String username = "root";
    private String remoteBooksPath = "/mnt/us/books";
    private AuthMode authMode = AuthMode.NO_PASSWORD;
    private String privateKeyPath = "";
    private String lastLocalFolder = "";

    public static AppConfig defaults() {
        return new AppConfig();
    }

    public String getHost() {
        return host == null ? "" : host;
    }

    public void setHost(String host) {
        this.host = host == null ? "" : host;
    }

    public int getPort() {
        return port <= 0 ? 2222 : port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username == null || username.isBlank() ? "root" : username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public String getRemoteBooksPath() {
        return remoteBooksPath == null || remoteBooksPath.isBlank() ? "/mnt/us/books" : remoteBooksPath;
    }

    public void setRemoteBooksPath(String remoteBooksPath) {
        this.remoteBooksPath = remoteBooksPath == null ? "" : remoteBooksPath;
    }

    public AuthMode getAuthMode() {
        return authMode == null ? AuthMode.NO_PASSWORD : authMode;
    }

    public void setAuthMode(AuthMode authMode) {
        this.authMode = authMode == null ? AuthMode.NO_PASSWORD : authMode;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath == null ? "" : privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath == null ? "" : privateKeyPath;
    }

    public String getLastLocalFolder() {
        return lastLocalFolder == null ? "" : lastLocalFolder;
    }

    public void setLastLocalFolder(String lastLocalFolder) {
        this.lastLocalFolder = lastLocalFolder == null ? "" : lastLocalFolder;
    }

    public enum AuthMode {
        NO_PASSWORD,
        PASSWORD,
        PRIVATE_KEY;

        public static AuthMode fromString(String value) {
            if (value == null || value.isBlank()) {
                return NO_PASSWORD;
            }
            String normalized = value.trim().replace(' ', '_').toUpperCase();
            for (AuthMode mode : values()) {
                if (mode.name().equals(normalized)) {
                    return mode;
                }
            }
            return NO_PASSWORD;
        }
    }
}
