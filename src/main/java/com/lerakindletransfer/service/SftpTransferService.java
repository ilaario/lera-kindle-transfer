package com.lerakindletransfer.service;

import com.lerakindletransfer.model.AppConfig;
import com.lerakindletransfer.model.Credentials;
import com.lerakindletransfer.model.TransferResult;
import com.lerakindletransfer.util.FileTypeValidator;
import com.lerakindletransfer.util.FilenameSanitizer;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SftpTransferService {
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int SESSION_TIMEOUT_MS = 30_000;
    private final LogService logService;

    public SftpTransferService(LogService logService) {
        this.logService = logService;
    }

    public void testConnection(AppConfig config, Credentials credentials) throws IOException {
        try (SSHClient ssh = connect(config, credentials);
             SFTPClient ignored = ssh.newSFTPClient()) {
            // Opening SFTP confirms that the authenticated session can transfer files.
        }
    }

    public List<TransferResult> uploadBooks(List<File> files, AppConfig config, Credentials credentials,
                                            ProgressCallback callback) throws IOException {
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(credentials, "credentials");
        ProgressCallback progress = callback == null ? new ProgressCallback() {
        } : callback;

        String remotePath = normalizeRemoteDirectory(config.getRemoteBooksPath());
        List<TransferResult> results = new ArrayList<>();

        try (SSHClient ssh = connect(config, credentials);
             SFTPClient sftp = ssh.newSFTPClient()) {
            ensureRemoteDirectoryExists(sftp, remotePath);

            int completed = 0;
            for (File file : files) {
                progress.onMessage("Uploading " + file.getName() + "...");
                try {
                    TransferResult result = uploadOne(sftp, remotePath, file);
                    results.add(result);
                    progress.onFileResult(result);
                } catch (IOException | RuntimeException ex) {
                    logService.error("Upload failed for " + file.getName(), ex);
                    TransferResult result = TransferResult.failure(file, userMessage(ex));
                    results.add(result);
                    progress.onFileResult(result);
                }
                completed++;
                progress.onProgress(completed, files.size());
            }
        }
        return results;
    }

    public void ensureRemoteDirectoryExists(SFTPClient sftp, String remotePath) throws IOException {
        sftp.mkdirs(remotePath);
    }

    private TransferResult uploadOne(SFTPClient sftp, String remotePath, File localFile) throws IOException {
        if (!localFile.isFile()) {
            throw new IOException("Local file is missing.");
        }
        if (!FileTypeValidator.isAccepted(localFile.toPath())) {
            throw new IOException("Unsupported file type.");
        }

        String sanitizedName = FilenameSanitizer.sanitize(localFile.getName());
        String remoteName = resolveDuplicateName(sanitizedName, candidateName ->
                remoteExists(sftp, joinRemote(remotePath, candidateName))
                        || remoteExists(sftp, joinRemote(remotePath, candidateName + ".part")));

        String finalPath = joinRemote(remotePath, remoteName);
        String partPath = joinRemote(remotePath, remoteName + ".part");
        long localSize = Files.size(localFile.toPath());

        try {
            sftp.put(localFile.getAbsolutePath(), partPath);
            long partSize = sftp.stat(partPath).getSize();
            if (partSize != localSize) {
                throw new IOException("Uploaded file size did not match local file size.");
            }
            if (remoteExists(sftp, finalPath)) {
                throw new IOException("A duplicate filename appeared during transfer; upload was not finalized.");
            }
            sftp.rename(partPath, finalPath);
            long finalSize = sftp.stat(finalPath).getSize();
            if (finalSize != localSize) {
                throw new IOException("Final uploaded file size did not match local file size.");
            }
            return TransferResult.success(localFile, remoteName);
        } catch (IOException | RuntimeException ex) {
            removePartFileQuietly(sftp, partPath);
            throw ex;
        }
    }

    SSHClient connect(AppConfig config, Credentials credentials) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.setConnectTimeout(CONNECT_TIMEOUT_MS);
        ssh.setTimeout(SESSION_TIMEOUT_MS);

        try {
            ssh.connect(config.getHost(), config.getPort());
            authenticate(ssh, config, credentials);
            return ssh;
        } catch (IOException | RuntimeException ex) {
            try {
                ssh.disconnect();
            } catch (IOException ignored) {
                // The original connection/authentication error is more useful.
            }
            throw ex;
        }
    }

    private void authenticate(SSHClient ssh, AppConfig config, Credentials credentials) throws IOException {
        switch (config.getAuthMode()) {
            case PASSWORD -> ssh.authPassword(config.getUsername(), credentials.password());
            case PRIVATE_KEY -> {
                KeyProvider keyProvider = credentials.privateKeyPassphrase().isBlank()
                        ? ssh.loadKeys(config.getPrivateKeyPath())
                        : ssh.loadKeys(config.getPrivateKeyPath(), credentials.privateKeyPassphrase());
                ssh.authPublickey(config.getUsername(), keyProvider);
            }
            case NO_PASSWORD -> ssh.authPassword(config.getUsername(), credentials.password().isBlank() ? "root" : credentials.password());
        }
    }

    public static String resolveDuplicateName(String desiredName, NameExists nameExists) throws IOException {
        String safeName = FilenameSanitizer.sanitize(desiredName);
        int dotIndex = safeName.lastIndexOf('.');
        String base = dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
        String extension = dotIndex > 0 ? safeName.substring(dotIndex) : "";

        for (int index = 0; index < 10_000; index++) {
            String candidate = index == 0 ? safeName : base + " (" + index + ")" + extension;
            if (!nameExists.exists(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Could not find an unused filename.");
    }

    private String normalizeRemoteDirectory(String remotePath) throws IOException {
        String trimmed = remotePath == null ? "" : remotePath.trim();
        if (trimmed.isBlank() || !trimmed.startsWith("/") || trimmed.contains("\0")) {
            throw new IOException("Remote folder must be an absolute folder such as /mnt/us/books.");
        }
        String[] parts = trimmed.split("/");
        for (String part : parts) {
            if (part.equals("..")) {
                throw new IOException("Remote folder cannot contain path traversal.");
            }
        }
        while (trimmed.length() > 1 && trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.equals("/")) {
            throw new IOException("Remote folder must be a books folder, not the filesystem root.");
        }
        return trimmed;
    }

    private boolean remoteExists(SFTPClient sftp, String path) throws IOException {
        return sftp.statExistence(path) != null;
    }

    private String joinRemote(String directory, String fileName) {
        return directory.endsWith("/") ? directory + fileName : directory + "/" + fileName;
    }

    private void removePartFileQuietly(SFTPClient sftp, String partPath) {
        try {
            if (remoteExists(sftp, partPath)) {
                sftp.rm(partPath);
            }
        } catch (IOException ignored) {
            // Best effort cleanup only; the user-facing failure remains the upload error.
        }
    }

    private String userMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? "Transfer failed." : message;
    }

    @FunctionalInterface
    public interface NameExists {
        boolean exists(String candidateName) throws IOException;
    }
}
