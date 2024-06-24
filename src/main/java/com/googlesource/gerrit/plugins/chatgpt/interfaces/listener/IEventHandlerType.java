package com.googlesource.gerrit.plugins.chatgpt.interfaces.listener;

public interface IEventHandlerType {
    enum PreprocessResult {
        OK, EXIT, SWITCH_TO_PATCH_SET_CREATED
    }

    PreprocessResult preprocessEvent();
    void processEvent() throws Exception;
}
