package com.lerakindletransfer.util;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FileTypeValidator {
    private static final List<String> EXTENSIONS = List.of(
            ".epub",
            ".pdf",
            ".mobi",
            ".azw3",
            ".cbz",
            ".cbr",
            ".txt",
            ".docx",
            ".fb2",
            ".djvu"
    );
    private static final Set<String> EXTENSION_SET = Set.copyOf(EXTENSIONS);

    private FileTypeValidator() {
    }

    public static boolean isAccepted(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return isAccepted(path.getFileName().toString());
    }

    public static boolean isAccepted(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == lower.length() - 1) {
            return false;
        }
        return EXTENSION_SET.contains(lower.substring(dotIndex));
    }

    public static List<String> acceptedGlobPatterns() {
        return EXTENSIONS.stream().map(extension -> "*" + extension).toList();
    }

    public static String acceptedExtensionsDescription() {
        return String.join(", ", EXTENSIONS);
    }
}
