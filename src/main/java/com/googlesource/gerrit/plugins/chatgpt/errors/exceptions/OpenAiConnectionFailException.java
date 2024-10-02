package com.googlesource.gerrit.plugins.chatgpt.errors.exceptions;

public class OpenAiConnectionFailException extends Exception {
    public OpenAiConnectionFailException() {
        super("Failed to connect to OpenAI services");
    }

    public OpenAiConnectionFailException(String message) {
        super(message);
    }

    public OpenAiConnectionFailException(Throwable cause) {
        super(cause);
    }
}
