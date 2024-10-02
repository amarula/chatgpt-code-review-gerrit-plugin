package com.googlesource.gerrit.plugins.chatgpt.errors.exceptions;

public class ResponseEmptyRepliesException extends OpenAiConnectionFailException {
    public ResponseEmptyRepliesException() {
        super("Invalid JSON format in response");
    }
}
