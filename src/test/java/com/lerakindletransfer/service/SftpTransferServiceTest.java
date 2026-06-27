package com.lerakindletransfer.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SftpTransferServiceTest {
    @Test
    void resolvesDuplicateNamesByAddingNumberBeforeExtension() throws IOException {
        Set<String> existing = new HashSet<>(Set.of("Book.epub", "Book (1).epub"));

        String resolved = SftpTransferService.resolveDuplicateName("Book.epub", existing::contains);

        assertEquals("Book (2).epub", resolved);
    }

    @Test
    void resolvesDuplicateNamesWithoutExtension() throws IOException {
        Set<String> existing = new HashSet<>(Set.of("Book"));

        String resolved = SftpTransferService.resolveDuplicateName("Book", existing::contains);

        assertEquals("Book (1)", resolved);
    }
}
