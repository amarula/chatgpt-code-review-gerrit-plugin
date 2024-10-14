package com.googlesource.gerrit.plugins.chatgpt.errors.exceptions;

public class CodeContextOnDemandLocatorException extends Exception {
    public CodeContextOnDemandLocatorException() {
        super("Failed retrieve Code Context Locator for On-Demand policy");
    }

    public CodeContextOnDemandLocatorException(String message) {
        super(message);
    }

    public CodeContextOnDemandLocatorException(Throwable cause) {
        super(cause);
    }
}
