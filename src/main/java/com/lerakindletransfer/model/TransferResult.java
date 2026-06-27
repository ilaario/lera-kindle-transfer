package com.lerakindletransfer.model;

import java.io.File;

public final class TransferResult {
    private final File localFile;
    private final String remoteFileName;
    private final boolean success;
    private final String message;

    private TransferResult(File localFile, String remoteFileName, boolean success, String message) {
        this.localFile = localFile;
        this.remoteFileName = remoteFileName;
        this.success = success;
        this.message = message;
    }

    public static TransferResult success(File localFile, String remoteFileName) {
        return new TransferResult(localFile, remoteFileName, true,
                "Uploaded " + localFile.getName() + " as " + remoteFileName + ".");
    }

    public static TransferResult failure(File localFile, String message) {
        return new TransferResult(localFile, "", false,
                "Failed " + localFile.getName() + ": " + message);
    }

    public File localFile() {
        return localFile;
    }

    public String remoteFileName() {
        return remoteFileName;
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }
}
