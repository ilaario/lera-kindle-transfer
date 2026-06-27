package com.lerakindletransfer.service;

import com.lerakindletransfer.util.MacPaths;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public final class KeyPairService {
    private static final String KEY_ALGORITHM = "EC";
    private static final String CURVE_NAME = "nistp256";
    private static final String JDK_CURVE_NAME = "secp256r1";
    private static final String SSH_KEY_TYPE = "ecdsa-sha2-" + CURVE_NAME;

    public LocalKeyPair ensureLocalKeyPair() throws IOException, GeneralSecurityException {
        Path keysDirectory = MacPaths.keysDirectory();
        Files.createDirectories(keysDirectory);
        Path privateKeyPath = MacPaths.privateKeyFile();
        Path publicKeyPath = MacPaths.publicKeyFile();

        if (Files.isRegularFile(privateKeyPath) && Files.isRegularFile(publicKeyPath)) {
            String publicKeyLine = Files.readString(publicKeyPath, StandardCharsets.UTF_8).trim();
            if (!publicKeyLine.isBlank()) {
                return new LocalKeyPair(privateKeyPath, publicKeyPath, publicKeyLine);
            }
        }

        KeyPair keyPair = generateEcKeyPair();
        String publicKeyLine = toAuthorizedKeysLine((ECPublicKey) keyPair.getPublic(), keyComment());
        writePrivateKey(privateKeyPath, keyPair);
        Files.writeString(publicKeyPath, publicKeyLine + System.lineSeparator(), StandardCharsets.UTF_8);
        restrictPrivateKeyPermissions(privateKeyPath);
        return new LocalKeyPair(privateKeyPath, publicKeyPath, publicKeyLine);
    }

    private KeyPair generateEcKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(new ECGenParameterSpec(JDK_CURVE_NAME));
        return generator.generateKeyPair();
    }

    private void writePrivateKey(Path privateKeyPath, KeyPair keyPair) throws IOException {
        String encoded = Base64.getMimeEncoder(64, System.lineSeparator().getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----" + System.lineSeparator()
                + encoded + System.lineSeparator()
                + "-----END PRIVATE KEY-----" + System.lineSeparator();
        Files.writeString(privateKeyPath, pem, StandardCharsets.US_ASCII);
    }

    private String toAuthorizedKeysLine(ECPublicKey publicKey, String comment) throws IOException {
        byte[] keyBlob = sshString(SSH_KEY_TYPE);
        byte[] curveBlob = sshString(CURVE_NAME);
        byte[] pointBlob = sshString(encodeEcPoint(publicKey));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(keyBlob);
        output.write(curveBlob);
        output.write(pointBlob);

        return SSH_KEY_TYPE + " "
                + Base64.getEncoder().encodeToString(output.toByteArray())
                + " " + comment;
    }

    private byte[] encodeEcPoint(ECPublicKey publicKey) {
        int fieldSizeBytes = (publicKey.getParams().getCurve().getField().getFieldSize() + 7) / 8;
        byte[] x = fixedLength(publicKey.getW().getAffineX(), fieldSizeBytes);
        byte[] y = fixedLength(publicKey.getW().getAffineY(), fieldSizeBytes);
        ByteBuffer buffer = ByteBuffer.allocate(1 + x.length + y.length);
        buffer.put((byte) 0x04);
        buffer.put(x);
        buffer.put(y);
        return buffer.array();
    }

    private byte[] fixedLength(BigInteger integer, int length) {
        byte[] bytes = integer.toByteArray();
        if (bytes.length == length) {
            return bytes;
        }
        byte[] fixed = new byte[length];
        int sourceOffset = Math.max(0, bytes.length - length);
        int copyLength = Math.min(bytes.length, length);
        System.arraycopy(bytes, sourceOffset, fixed, length - copyLength, copyLength);
        return fixed;
    }

    private byte[] sshString(String value) throws IOException {
        return sshString(value.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] sshString(byte[] value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(ByteBuffer.allocate(4).putInt(value.length).array());
        output.write(value);
        return output.toByteArray();
    }

    private String keyComment() {
        String user = System.getProperty("user.name", "user").replaceAll("\\s+", "_");
        String host = "mac";
        try {
            host = InetAddress.getLocalHost().getHostName().replaceAll("\\s+", "_");
        } catch (RuntimeException | IOException ignored) {
            // A readable fallback is enough for an authorized_keys comment.
        }
        return "lera-kindle-transfer-" + user + "@" + host;
    }

    private void restrictPrivateKeyPermissions(Path privateKeyPath) {
        try {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(privateKeyPath, permissions);
        } catch (IOException | UnsupportedOperationException ignored) {
            // Best effort on non-POSIX filesystems.
        }
    }

    public record LocalKeyPair(Path privateKeyPath, Path publicKeyPath, String publicKeyLine) {
    }
}
