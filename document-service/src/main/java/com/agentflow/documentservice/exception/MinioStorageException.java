package com.agentflow.documentservice.exception;

public class MinioStorageException extends RuntimeException {
    public MinioStorageException(String message, Exception e) {
        super(message);
    }
}
