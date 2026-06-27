package com.lerakindletransfer.util;

import java.text.Normalizer;

public final class FilenameSanitizer {
    private static final int MAX_FILENAME_LENGTH = 180;

    private FilenameSanitizer() {
    }

    public static String sanitize(String originalName) {
        String name = originalName == null ? "" : originalName.trim();
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = Normalizer.normalize(name, Normalizer.Form.NFC);
        name = name.replaceAll("[\\p{Cntrl}:*?\"<>|/]", "_");
        name = name.replaceAll("\\s+", " ").trim();
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        while (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isBlank() || name.equals("..")) {
            name = "book";
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            name = shortenPreservingExtension(name);
        }
        return name;
    }

    private static String shortenPreservingExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < name.length() - 1) {
            String base = name.substring(0, dotIndex);
            String extension = name.substring(dotIndex);
            int maxBaseLength = Math.max(1, MAX_FILENAME_LENGTH - extension.length());
            return base.substring(0, Math.min(base.length(), maxBaseLength)).trim() + extension;
        }
        return name.substring(0, MAX_FILENAME_LENGTH).trim();
    }
}
