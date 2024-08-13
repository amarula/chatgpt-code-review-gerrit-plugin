package com.googlesource.gerrit.plugins.chatgpt.exceptions;

public class ResponseEmptyRepliesException extends OpenAiConnectionFailException {
    public ResponseEmptyRepliesException() {
        super("Invalid JSON format in response");
    }
}
