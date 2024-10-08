package com.googlesource.gerrit.plugins.chatgpt.errors.exceptions;

public class OperationNotSupportedException extends Exception {
    public OperationNotSupportedException() {
        super("Operation not supported");
    }

    public OperationNotSupportedException(String message) {
        super(message);
    }

    public OperationNotSupportedException(Throwable cause) {
        super(cause);
    }
}
