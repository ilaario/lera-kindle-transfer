package com.lerakindletransfer.service;

import com.lerakindletransfer.util.MacPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LogService {
    private final Logger logger = Logger.getLogger("LeraKindleTransfer");

    public LogService() {
        try {
            Files.createDirectories(MacPaths.logsDirectory());
            logger.setUseParentHandlers(false);
            if (logger.getHandlers().length == 0) {
                FileHandler handler = new FileHandler(MacPaths.logFile().toString(), true);
                handler.setFormatter(new SimpleFormatter());
                logger.addHandler(handler);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not initialize file logging", ex);
        }
    }

    public void info(String message) {
        logger.info(message);
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
