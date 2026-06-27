package com.lerakindletransfer.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FilenameSanitizerTest {
    @Test
    void removesPathTraversalAndProblemCharacters() {
        assertEquals("Book_.epub", FilenameSanitizer.sanitize("../Book?.epub"));
        assertEquals("Book.epub", FilenameSanitizer.sanitize("/tmp/../../Book.epub"));
        assertEquals("book", FilenameSanitizer.sanitize("../"));
    }

    @Test
    void keepsUnicodeLetters() {
        assertEquals("Книга.epub", FilenameSanitizer.sanitize("Книга.epub"));
    }

    @Test
    void preventsAbsolutePathInjection() {
        String sanitized = FilenameSanitizer.sanitize("/mnt/us/books/Evil.pdf");

        assertEquals("Evil.pdf", sanitized);
        assertFalse(sanitized.contains("/"));
    }
}
