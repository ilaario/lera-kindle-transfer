package com.lerakindletransfer.model;

import java.nio.file.Path;

public record SecureInitResult(Path privateKeyPath, String publicKeyLine, String authorizedKeysPath, boolean keyAlreadyPresent) {
}
