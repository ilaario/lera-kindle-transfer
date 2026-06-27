package com.lerakindletransfer.service;

import com.lerakindletransfer.model.TransferResult;

public interface ProgressCallback {
    default void onMessage(String message) {
    }

    default void onProgress(int completedFiles, int totalFiles) {
    }

    default void onFileResult(TransferResult result) {
    }
}
