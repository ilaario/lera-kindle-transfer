package com.lerakindletransfer.service;

import net.schmizz.sshj.SSHClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyPairServiceTest {
    @TempDir
    Path home;

    @Test
    void createsEcKeyPairThatSshjCanLoad() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        try {
            KeyPairService.LocalKeyPair keyPair = new KeyPairService().ensureLocalKeyPair();

            assertTrue(Files.isRegularFile(keyPair.privateKeyPath()));
            assertTrue(Files.isRegularFile(keyPair.publicKeyPath()));
            assertTrue(keyPair.publicKeyLine().startsWith("ecdsa-sha2-nistp256 "));

            SSHClient sshClient = new SSHClient();
            assertDoesNotThrow(() -> sshClient.loadKeys(keyPair.privateKeyPath().toString()));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void reusesExistingPublicKeyLine() throws Exception {
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        try {
            KeyPairService service = new KeyPairService();
            KeyPairService.LocalKeyPair first = service.ensureLocalKeyPair();
            KeyPairService.LocalKeyPair second = service.ensureLocalKeyPair();

            assertEquals(first.publicKeyLine(), second.publicKeyLine());
            assertEquals(first.privateKeyPath(), second.privateKeyPath());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
