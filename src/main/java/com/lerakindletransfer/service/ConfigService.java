package com.lerakindletransfer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lerakindletransfer.model.AppConfig;
import com.lerakindletransfer.util.MacPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final LogService logService;

    public ConfigService(LogService logService) {
        this.logService = logService;
    }

    public AppConfig load() {
        Path configFile = MacPaths.configFile();
        if (!Files.exists(configFile)) {
            AppConfig defaults = AppConfig.defaults();
            save(defaults);
            return defaults;
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            AppConfig loaded = gson.fromJson(reader, AppConfig.class);
            return loaded == null ? AppConfig.defaults() : loaded;
        } catch (IOException | RuntimeException ex) {
            logService.error("Could not load config, using defaults", ex);
            return AppConfig.defaults();
        }
    }

    public void save(AppConfig config) {
        Path configFile = MacPaths.configFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(config, writer);
            }
        } catch (IOException ex) {
            logService.error("Could not save config", ex);
        }
    }
}
