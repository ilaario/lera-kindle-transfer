package com.lerakindletransfer.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeValidatorTest {
    @Test
    void acceptsSupportedExtensionsCaseInsensitively() {
        assertTrue(FileTypeValidator.isAccepted(Path.of("Novel.EPUB")));
        assertTrue(FileTypeValidator.isAccepted(Path.of("Manual.pdf")));
        assertTrue(FileTypeValidator.isAccepted(Path.of("Comic.CBZ")));
    }

    @Test
    void rejectsUnsupportedExtensions() {
        assertFalse(FileTypeValidator.isAccepted(Path.of("cover.png")));
        assertFalse(FileTypeValidator.isAccepted(Path.of("archive.zip")));
        assertFalse(FileTypeValidator.isAccepted(Path.of("no-extension")));
        assertFalse(FileTypeValidator.isAccepted(Path.of(".epub")));
    }
}
