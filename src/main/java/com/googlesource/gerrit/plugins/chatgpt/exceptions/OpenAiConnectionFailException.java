package com.googlesource.gerrit.plugins.chatgpt.exceptions;

public class OpenAiConnectionFailException extends Exception {
    public OpenAiConnectionFailException() {
        super("Failed to connect to OpenAI services");
    }

    public OpenAiConnectionFailException(Throwable cause) {
        super(cause);
    }
}
