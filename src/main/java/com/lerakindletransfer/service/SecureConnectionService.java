package com.lerakindletransfer.service;

import com.lerakindletransfer.model.AppConfig;
import com.lerakindletransfer.model.Credentials;
import com.lerakindletransfer.model.SecureInitResult;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.SFTPClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.function.Consumer;

public final class SecureConnectionService {
    private static final String KINDLE_AUTHORIZED_KEYS_PATH = "/mnt/us/koreader/settings/SSH/authorized_keys";
    private static final String KOBO_AUTHORIZED_KEYS_PATH = "/mnt/onboard/.adds/koreader/settings/SSH/authorized_keys";

    private final SftpTransferService transferService;
    private final KeyPairService keyPairService;
    private final LogService logService;

    public SecureConnectionService(SftpTransferService transferService, KeyPairService keyPairService, LogService logService) {
        this.transferService = transferService;
        this.keyPairService = keyPairService;
        this.logService = logService;
    }

    public SecureInitResult installPublicKey(AppConfig config, Consumer<String> progress)
            throws IOException, GeneralSecurityException {
        AppConfig bootstrapConfig = copyForPasswordBootstrap(config);
        KeyPairService.LocalKeyPair keyPair = keyPairService.ensureLocalKeyPair();
        String authorizedKeysPath = authorizedKeysPathFor(config);
        String authorizedKeysDirectory = parentDirectory(authorizedKeysPath);

        message(progress, "Connecting with KOReader temporary root/root access...");
        try (SSHClient ssh = transferService.connect(bootstrapConfig, new Credentials("root", ""));
             SFTPClient sftp = ssh.newSFTPClient()) {
            message(progress, "Creating KOReader SSH key folder if needed...");
            sftp.mkdirs(authorizedKeysDirectory);

            String existingKeys = readRemoteTextIfExists(sftp, authorizedKeysPath);
            if (containsPublicKey(existingKeys, keyPair.publicKeyLine())) {
                message(progress, "This laptop key is already installed on KOReader.");
                return new SecureInitResult(keyPair.privateKeyPath(), keyPair.publicKeyLine(), authorizedKeysPath, true);
            }

            message(progress, "Appending this laptop public key to KOReader authorized_keys...");
            appendLine(sftp, authorizedKeysPath, existingKeys, keyPair.publicKeyLine());
            try {
                sftp.chmod(authorizedKeysPath, 0600);
            } catch (IOException ex) {
                logService.error("Could not chmod authorized_keys", ex);
            }
            return new SecureInitResult(keyPair.privateKeyPath(), keyPair.publicKeyLine(), authorizedKeysPath, false);
        }
    }

    public void testPrivateKeyConnection(AppConfig config, Path privateKeyPath) throws IOException {
        AppConfig keyConfig = copyForPrivateKey(config, privateKeyPath);
        transferService.testConnection(keyConfig, new Credentials("", ""));
    }

    public String authorizedKeysPathFor(AppConfig config) {
        String remoteBooksPath = config.getRemoteBooksPath();
        if (remoteBooksPath.startsWith("/mnt/onboard")) {
            return KOBO_AUTHORIZED_KEYS_PATH;
        }
        return KINDLE_AUTHORIZED_KEYS_PATH;
    }

    private AppConfig copyForPasswordBootstrap(AppConfig config) {
        AppConfig copy = copyBase(config);
        copy.setUsername("root");
        copy.setAuthMode(AppConfig.AuthMode.PASSWORD);
        copy.setPrivateKeyPath("");
        return copy;
    }

    private AppConfig copyForPrivateKey(AppConfig config, Path privateKeyPath) {
        AppConfig copy = copyBase(config);
        copy.setUsername("root");
        copy.setAuthMode(AppConfig.AuthMode.PRIVATE_KEY);
        copy.setPrivateKeyPath(privateKeyPath.toString());
        return copy;
    }

    private AppConfig copyBase(AppConfig config) {
        AppConfig copy = new AppConfig();
        copy.setHost(config.getHost());
        copy.setPort(config.getPort());
        copy.setUsername(config.getUsername());
        copy.setRemoteBooksPath(config.getRemoteBooksPath());
        copy.setAuthMode(config.getAuthMode());
        copy.setPrivateKeyPath(config.getPrivateKeyPath());
        copy.setLastLocalFolder(config.getLastLocalFolder());
        return copy;
    }

    private String readRemoteTextIfExists(SFTPClient sftp, String remotePath) throws IOException {
        if (sftp.statExistence(remotePath) == null) {
            return "";
        }
        Path tempFile = Files.createTempFile("lera-kindle-authorized-keys", ".tmp");
        try {
            sftp.get(remotePath, tempFile.toString());
            return Files.readString(tempFile, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private boolean containsPublicKey(String authorizedKeys, String publicKeyLine) {
        String publicKeyWithoutComment = firstTwoFields(publicKeyLine);
        return authorizedKeys.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(this::firstTwoFields)
                .anyMatch(publicKeyWithoutComment::equals);
    }

    private String firstTwoFields(String keyLine) {
        String[] fields = keyLine.trim().split("\\s+");
        if (fields.length < 2) {
            return keyLine.trim();
        }
        return fields[0] + " " + fields[1];
    }

    private void appendLine(SFTPClient sftp, String remotePath, String existingText, String line) throws IOException {
        String prefix = existingText.isBlank() || existingText.endsWith("\n") ? "" : "\n";
        byte[] bytes = (prefix + line + "\n").getBytes(StandardCharsets.UTF_8);
        try (RemoteFile remoteFile = sftp.open(remotePath, EnumSet.of(OpenMode.WRITE, OpenMode.CREAT, OpenMode.APPEND))) {
            remoteFile.write(remoteFile.length(), bytes, 0, bytes.length);
        }
    }

    private String parentDirectory(String remotePath) throws IOException {
        int slash = remotePath.lastIndexOf('/');
        if (slash <= 0) {
            throw new IOException("Invalid KOReader authorized_keys path.");
        }
        return remotePath.substring(0, slash);
    }

    private void message(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }
}
