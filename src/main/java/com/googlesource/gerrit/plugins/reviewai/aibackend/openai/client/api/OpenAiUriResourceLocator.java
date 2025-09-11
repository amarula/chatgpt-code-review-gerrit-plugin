/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.api;

public class OpenAiUriResourceLocator {
  private static final String VERSION_URI = "/v1";

  public static String filesCreateUri() {
    return VERSION_URI + "/files";
  }

  public static String assistantCreateUri() {
    return VERSION_URI + "/assistants";
  }

  public static String threadsUri() {
    return VERSION_URI + "/threads";
  }

  public static String threadRetrieveUri(String threadId) {
    return threadsUri() + "/" + threadId;
  }

  public static String threadMessagesUri(String threadId) {
    return threadRetrieveUri(threadId) + "/messages";
  }

  public static String threadMessageRetrieveUri(String threadId, String messageId) {
    return threadMessagesUri(threadId) + "/" + messageId;
  }

  public static String runsUri(String threadId) {
    return threadRetrieveUri(threadId) + "/runs";
  }

  public static String runRetrieveUri(String threadId, String runId) {
    return runsUri(threadId) + "/" + runId;
  }

  public static String runStepsUri(String threadId, String runId) {
    return runRetrieveUri(threadId, runId) + "/steps";
  }

  public static String runCancelUri(String threadId, String runId) {
    return runRetrieveUri(threadId, runId) + "/cancel";
  }

  public static String runSubmitToolOutputsUri(String threadId, String runId) {
    return runRetrieveUri(threadId, runId) + "/submit_tool_outputs";
  }

  public static String vectorStoreCreateUri() {
    return VERSION_URI + "/vector_stores";
  }

  public static String vectorStoreFileBatchCreateUri(String vectorStoreId) {
    return vectorStoreCreateUri() + "/" + vectorStoreId + "/file_batches";
  }

  public static String vectorStoreFileBatchRetrieveUri(
      String vectorStoreId, String vectorStoreFileBatchId) {
    return vectorStoreFileBatchCreateUri(vectorStoreId) + "/" + vectorStoreFileBatchId;
  }
}
