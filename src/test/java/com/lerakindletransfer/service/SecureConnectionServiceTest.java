package com.lerakindletransfer.service;

import com.lerakindletransfer.model.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecureConnectionServiceTest {
    @Test
    void usesKindleAuthorizedKeysPathForMntUs() {
        AppConfig config = new AppConfig();
        config.setRemoteBooksPath("/mnt/us/books");

        SecureConnectionService service = new SecureConnectionService(null, null, null);

        assertEquals("/mnt/us/koreader/settings/SSH/authorized_keys", service.authorizedKeysPathFor(config));
    }

    @Test
    void usesKoboAuthorizedKeysPathForMntOnboard() {
        AppConfig config = new AppConfig();
        config.setRemoteBooksPath("/mnt/onboard/books");

        SecureConnectionService service = new SecureConnectionService(null, null, null);

        assertEquals("/mnt/onboard/.adds/koreader/settings/SSH/authorized_keys", service.authorizedKeysPathFor(config));
    }
}
