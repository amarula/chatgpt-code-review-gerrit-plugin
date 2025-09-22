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

package com.googlesource.gerrit.plugins.reviewai.config;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.util.OneOffRequestContext;

import java.util.*;

import static com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPrompt.getJsonPromptValues;
import static com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.CodeContextPolicyBase.CodeContextPolicies;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.AiBackends;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;

public class Configuration extends ConfigCore {
  // Config Constants
  public static final String DEFAULT_EMPTY_SETTING = "";
  public static final String ENABLED_USERS_ALL = "ALL";
  public static final String ENABLED_GROUPS_ALL = "ALL";
  public static final String ENABLED_TOPICS_ALL = "ALL";

  // Default Config values
  public static final String OPENAI_DOMAIN = "https://api.openai.com";
  public static final String GEMINI_DOMAIN = "https://generativelanguage.googleapis.com";
  public static final String MOONSHOT_DOMAIN = "https://api.moonshot.ai";
  public static final String DEFAULT_AI_MODEL = "gpt-4o";
  public static final String DEFAULT_GEMINI_AI_MODEL = "gemini-2.5-flash";
  public static final String DEFAULT_MOONSHOT_AI_MODEL = "moonshot-v1-8k";
  public static final double DEFAULT_AI_REVIEW_TEMPERATURE = 0.2;
  public static final double DEFAULT_AI_COMMENT_TEMPERATURE = 1.0;

  private static final String DEFAULT_AI_BACKEND = "OPENAI";
  private static final String DEFAULT_LC_PROVIDER = "OPENAI";
  private static final boolean DEFAULT_REVIEW_PATCH_SET = true;
  private static final boolean DEFAULT_REVIEW_COMMIT_MESSAGES = true;
  private static final boolean DEFAULT_FULL_FILE_REVIEW = true;
  private static final String DEFAULT_CODE_CONTEXT_POLICY = "UPLOAD_ALL";
  private static final String DEFAULT_CODE_CONTEXT_ON_DEMAND_BASE_PATH = "";
  private static final boolean DEFAULT_GLOBAL_ENABLE = false;
  private static final String DEFAULT_DISABLED_USERS = "";
  private static final String DEFAULT_ENABLED_USERS = ENABLED_USERS_ALL;
  private static final String DEFAULT_DISABLED_GROUPS = "";
  private static final String DEFAULT_ENABLED_GROUPS = ENABLED_GROUPS_ALL;
  private static final String DEFAULT_DISABLED_TOPIC_FILTER = "";
  private static final String DEFAULT_ENABLED_TOPIC_FILTER = ENABLED_TOPICS_ALL;
  private static final String DEFAULT_ENABLED_PROJECTS = "";
  private static final String DEFAULT_ENABLED_FILE_EXTENSIONS =
      String.join(
          ",",
          new String[] {
            ".py", ".java", ".js", ".ts", ".html", ".css", ".cs", ".cpp", ".c", ".h", ".php", ".rb",
            ".swift", ".kt", ".r", ".jl", ".go", ".scala", ".pl", ".pm", ".rs", ".dart", ".lua",
            ".sh", ".vb", ".bat"
          });
  private static final boolean DEFAULT_PROJECT_ENABLE = false;
  private static final List<String> DEFAULT_DIRECTIVES = new ArrayList<>();
  private static final int DEFAULT_MAX_REVIEW_LINES = 1000;
  private static final boolean DEFAULT_ENABLED_VOTING = false;
  private static final boolean DEFAULT_FILTER_NEGATIVE_COMMENTS = true;
  private static final int DEFAULT_FILTER_COMMENTS_BELOW_SCORE = 0;
  private static final boolean DEFAULT_FILTER_RELEVANT_COMMENTS = true;
  private static final double DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD = 0.6;
  private static final int DEFAULT_VOTING_MIN_SCORE = -1;
  private static final int DEFAULT_VOTING_MAX_SCORE = 1;
  private static final boolean DEFAULT_INLINE_COMMENTS_AS_RESOLVED = false;
  private static final boolean DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED = false;
  private static final boolean DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS = false;
  private static final boolean DEFAULT_IGNORE_RESOLVED_AI_COMMENTS = true;
  private static final boolean DEFAULT_FORCE_CREATE_ASSISTANT = false;
  private static final boolean DEFAULT_TASK_SPECIFIC_ASSISTANTS = false;
  private static final int DEFAULT_AI_CONNECTION_TIMEOUT = 30;
  private static final int DEFAULT_AI_CONNECTION_RETRY_INTERVAL = 10;
  private static final int DEFAULT_AI_CONNECTION_MAX_RETRY_ATTEMPTS = 2;
  private static final int DEFAULT_AI_POLLING_TIMEOUT = 180;
  private static final int DEFAULT_AI_POLLING_INTERVAL = 1000;
  private static final int DEFAULT_AI_UPLOADED_CHUNK_SIZE_MB = 5;
  private static final int DEFAULT_LC_MAX_MEMORY_TOKENS = 16384;
  private static final boolean DEFAULT_ENABLE_MESSAGE_DEBUGGING = false;
  private static final List<String> DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE = new ArrayList<>();

  // Config setting keys
  public static final String KEY_AI_SYSTEM_PROMPT_INSTRUCTIONS = "aiSystemPromptInstructions";
  public static final String KEY_AI_RELEVANCE_RULES = "aiRelevanceRules";
  public static final String KEY_AI_REVIEW_TEMPERATURE = "aiReviewTemperature";
  public static final String KEY_AI_COMMENT_TEMPERATURE = "aiCommentTemperature";
  public static final String KEY_DIRECTIVES = "directive";
  public static final String KEY_VOTING_MIN_SCORE = "votingMinScore";
  public static final String KEY_VOTING_MAX_SCORE = "votingMaxScore";
  public static final String KEY_GERRIT_USERNAME = "gerritUserName";
  public static final String KEY_SELECTIVE_LOG_LEVEL_OVERRIDE = "selectiveLogLevelOverride";

  // Config entry keys with list values
  public static final Set<String> LIST_TYPE_ENTRY_KEYS =
      Set.of(KEY_DIRECTIVES, KEY_SELECTIVE_LOG_LEVEL_OVERRIDE);

  private static final String KEY_AI_TOKEN = "aiToken";
  private static final String KEY_AI_DOMAIN = "aiDomain";
  private static final String KEY_AI_MODEL = "aiModel";
  private static final String KEY_AI_BACKEND = "aiBackend";
  private static final String KEY_REVIEW_COMMIT_MESSAGES = "aiReviewCommitMessages";
  private static final String KEY_REVIEW_PATCH_SET = "aiReviewPatchSet";
  private static final String KEY_FULL_FILE_REVIEW = "aiFullFileReview";
  private static final String KEY_CODE_CONTEXT_POLICY = "codeContextPolicy";
  private static final String KEY_CODE_CONTEXT_ON_DEMAND_BASE_PATH = "codeContextOnDemandBasePath";
  private static final String KEY_PROJECT_ENABLE = "isEnabled";
  private static final String KEY_GLOBAL_ENABLE = "globalEnable";
  private static final String KEY_DISABLED_USERS = "disabledUsers";
  private static final String KEY_ENABLED_USERS = "enabledUsers";
  private static final String KEY_DISABLED_GROUPS = "disabledGroups";
  private static final String KEY_ENABLED_GROUPS = "enabledGroups";
  private static final String KEY_DISABLED_TOPIC_FILTER = "disabledTopicFilter";
  private static final String KEY_ENABLED_TOPIC_FILTER = "enabledTopicFilter";
  private static final String KEY_ENABLED_PROJECTS = "enabledProjects";
  private static final String KEY_MAX_REVIEW_LINES = "maxReviewLines";
  private static final String KEY_ENABLED_FILE_EXTENSIONS = "enabledFileExtensions";
  private static final String KEY_ENABLED_VOTING = "enabledVoting";
  private static final String KEY_FILTER_NEGATIVE_COMMENTS = "filterNegativeComments";
  private static final String KEY_FILTER_COMMENTS_BELOW_SCORE = "filterCommentsBelowScore";
  private static final String KEY_FILTER_RELEVANT_COMMENTS = "filterRelevantComments";
  private static final String KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD =
      "filterCommentsRelevanceThreshold";
  private static final String KEY_LC_MAX_MEMORY_TOKENS = "lcMaxMemoryTokens";
  private static final String KEY_LC_PROVIDER = "lcProvider";
  private static final String KEY_INLINE_COMMENTS_AS_RESOLVED = "inlineCommentsAsResolved";
  private static final String KEY_PATCH_SET_COMMENTS_AS_RESOLVED = "patchSetCommentsAsResolved";
  private static final String KEY_IGNORE_OUTDATED_INLINE_COMMENTS = "ignoreOutdatedInlineComments";
  private static final String KEY_IGNORE_RESOLVED_AI_COMMENTS = "ignoreResolvedAiComments";
  private static final String KEY_FORCE_CREATE_ASSISTANT = "forceCreateAssistant";
  private static final String KEY_TASK_SPECIFIC_ASSISTANTS = "taskSpecificAssistants";
  private static final String KEY_AI_CONNECTION_TIMEOUT = "aiConnectionTimeout";
  private static final String KEY_AI_CONNECTION_RETRY_INTERVAL = "aiConnectionRetryInterval";
  private static final String KEY_AI_CONNECTION_MAX_RETRY_ATTEMPTS = "aiConnectionMaxRetryAttempts";
  private static final String KEY_AI_POLLING_TIMEOUT = "aiPollingTimeout";
  private static final String KEY_AI_POLLING_INTERVAL = "aiPollingInterval";
  private static final String KEY_AI_UPLOADED_CHUNK_SIZE_MB = "aiUploadedChunkSizeMb";
  private static final String KEY_ENABLE_MESSAGE_DEBUGGING = "enableMessageDebugging";

  public Configuration(
      OneOffRequestContext context,
      GerritApi gerritApi,
      PluginConfig globalConfig,
      PluginConfig projectConfig,
      String gerritUserEmail,
      Account.Id userId) {
    super(context, gerritApi, globalConfig, projectConfig, gerritUserEmail, userId);
  }

  public String getAiToken() {
    return getValidatedOrThrow(KEY_AI_TOKEN);
  }

  public String getGerritUserName() {
    return getValidatedOrThrow(KEY_GERRIT_USERNAME);
  }

  public String getAiDomain() {
    String aiDomain = getString(KEY_AI_DOMAIN);
    if (aiDomain != null && !aiDomain.isEmpty()) {
      return aiDomain;
    }

    if (getAiBackend() == AiBackends.LANGCHAIN) {
      return getDefaultLangChainDomain();
    }

    return OPENAI_DOMAIN;
  }

  public String getAiModel() {
    String model = getString(KEY_AI_MODEL);
    if (model != null && !model.isEmpty()) {
      return model;
    }

    if (getAiBackend() == AiBackends.LANGCHAIN) {
      return getDefaultLangChainModel();
    }

    return DEFAULT_AI_MODEL;
  }

  // The default system prompt/instructions are specified in the prompt files and are passed as a
  // parameter
  public String getAiSystemPromptInstructions(String defaultAiSystemPromptInstructions) {
    return getString(KEY_AI_SYSTEM_PROMPT_INSTRUCTIONS, defaultAiSystemPromptInstructions);
  }

  // If the default system prompt/instructions are not available in the caller's scope (e.g., when
  // displaying the configuration after a command request), they are retrieved from the prompt
  // files.
  public String getAiSystemPromptInstructions() {
    Map<String, Object> systemPrompts = getJsonPromptValues("prompts");
    return getAiSystemPromptInstructions(
        systemPrompts.get("DEFAULT_AI_SYSTEM_PROMPT_INSTRUCTIONS").toString());
  }

  public boolean getAiReviewPatchSet() {
    return getBoolean(KEY_REVIEW_PATCH_SET, DEFAULT_REVIEW_PATCH_SET);
  }

  public AiBackends getAiBackend() {
    return getEnum(KEY_AI_BACKEND, DEFAULT_AI_BACKEND, AiBackends.class);
  }

  public LangChainProviders getLcProvider() {
    return getEnum(KEY_LC_PROVIDER, DEFAULT_LC_PROVIDER, LangChainProviders.class);
  }

  public boolean getAiReviewCommitMessages() {
    return getBoolean(KEY_REVIEW_COMMIT_MESSAGES, DEFAULT_REVIEW_COMMIT_MESSAGES);
  }

  public boolean getAiFullFileReview() {
    return getBoolean(KEY_FULL_FILE_REVIEW, DEFAULT_FULL_FILE_REVIEW);
  }

  public CodeContextPolicies getCodeContextPolicy() {
    return getEnum(KEY_CODE_CONTEXT_POLICY, DEFAULT_CODE_CONTEXT_POLICY, CodeContextPolicies.class);
  }

  public String getCodeContextOnDemandBasePath() {
    return getString(
        KEY_CODE_CONTEXT_ON_DEMAND_BASE_PATH, DEFAULT_CODE_CONTEXT_ON_DEMAND_BASE_PATH);
  }

  public boolean isProjectEnable() {
    return projectConfig.getBoolean(KEY_PROJECT_ENABLE, DEFAULT_PROJECT_ENABLE);
  }

  public boolean isGlobalEnable() {
    return globalConfig.getBoolean(KEY_GLOBAL_ENABLE, DEFAULT_GLOBAL_ENABLE);
  }

  public List<String> getDisabledUsers() {
    return splitConfig(getString(KEY_DISABLED_USERS, DEFAULT_DISABLED_USERS));
  }

  public List<String> getEnabledUsers() {
    return splitConfig(getString(KEY_ENABLED_USERS, DEFAULT_ENABLED_USERS));
  }

  public List<String> getDisabledGroups() {
    return splitConfig(getString(KEY_DISABLED_GROUPS, DEFAULT_DISABLED_GROUPS));
  }

  public List<String> getEnabledGroups() {
    return splitConfig(getString(KEY_ENABLED_GROUPS, DEFAULT_ENABLED_GROUPS));
  }

  public List<String> getDisabledTopicFilter() {
    return splitConfig(getString(KEY_DISABLED_TOPIC_FILTER, DEFAULT_DISABLED_TOPIC_FILTER));
  }

  public List<String> getEnabledTopicFilter() {
    return splitConfig(getString(KEY_ENABLED_TOPIC_FILTER, DEFAULT_ENABLED_TOPIC_FILTER));
  }

  public String getEnabledProjects() {
    return globalConfig.getString(KEY_ENABLED_PROJECTS, DEFAULT_ENABLED_PROJECTS);
  }

  public int getMaxReviewLines() {
    return getInt(KEY_MAX_REVIEW_LINES, DEFAULT_MAX_REVIEW_LINES);
  }

  public List<String> getEnabledFileExtensions() {
    return splitConfigRemoveDots(
        getString(KEY_ENABLED_FILE_EXTENSIONS, DEFAULT_ENABLED_FILE_EXTENSIONS));
  }

  public List<String> getDirective() {
    return splitListIntoItems(KEY_DIRECTIVES, DEFAULT_DIRECTIVES);
  }

  public boolean isVotingEnabled() {
    return getBoolean(KEY_ENABLED_VOTING, DEFAULT_ENABLED_VOTING);
  }

  public boolean getFilterNegativeComments() {
    return getBoolean(KEY_FILTER_NEGATIVE_COMMENTS, DEFAULT_FILTER_NEGATIVE_COMMENTS);
  }

  public int getFilterCommentsBelowScore() {
    return getInt(KEY_FILTER_COMMENTS_BELOW_SCORE, DEFAULT_FILTER_COMMENTS_BELOW_SCORE);
  }

  public boolean getFilterRelevantComments() {
    return getBoolean(KEY_FILTER_RELEVANT_COMMENTS, DEFAULT_FILTER_RELEVANT_COMMENTS);
  }

  public double getFilterCommentsRelevanceThreshold() {
    return getDouble(
        KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD, DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD);
  }

  public String getAiRelevanceRules() {
    return getString(KEY_AI_RELEVANCE_RULES, DEFAULT_EMPTY_SETTING);
  }

  public String getAiReviewTemperature() {
    return getString(KEY_AI_REVIEW_TEMPERATURE, String.valueOf(DEFAULT_AI_REVIEW_TEMPERATURE));
  }

  public String getAiCommentTemperature() {
    return getString(KEY_AI_COMMENT_TEMPERATURE, String.valueOf(DEFAULT_AI_COMMENT_TEMPERATURE));
  }

  public int getVotingMinScore() {
    return getInt(KEY_VOTING_MIN_SCORE, DEFAULT_VOTING_MIN_SCORE);
  }

  public int getVotingMaxScore() {
    return getInt(KEY_VOTING_MAX_SCORE, DEFAULT_VOTING_MAX_SCORE);
  }

  public boolean getInlineCommentsAsResolved() {
    return getBoolean(KEY_INLINE_COMMENTS_AS_RESOLVED, DEFAULT_INLINE_COMMENTS_AS_RESOLVED);
  }

  public boolean getPatchSetCommentsAsResolved() {
    return getBoolean(KEY_PATCH_SET_COMMENTS_AS_RESOLVED, DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED);
  }

  public boolean getIgnoreResolvedAiComments() {
    return getBoolean(KEY_IGNORE_RESOLVED_AI_COMMENTS, DEFAULT_IGNORE_RESOLVED_AI_COMMENTS);
  }

  public boolean getForceCreateAssistant() {
    return getBoolean(KEY_FORCE_CREATE_ASSISTANT, DEFAULT_FORCE_CREATE_ASSISTANT);
  }

  public boolean getTaskSpecificAssistants() {
    return getBoolean(KEY_TASK_SPECIFIC_ASSISTANTS, DEFAULT_TASK_SPECIFIC_ASSISTANTS);
  }

  public int getAiConnectionTimeout() {
    return getInt(KEY_AI_CONNECTION_TIMEOUT, DEFAULT_AI_CONNECTION_TIMEOUT);
  }

  public int getLcMaxMemoryTokens() {
    return getInt(KEY_LC_MAX_MEMORY_TOKENS, DEFAULT_LC_MAX_MEMORY_TOKENS);
  }

  public int getAiConnectionRetryInterval() {
    return getInt(KEY_AI_CONNECTION_RETRY_INTERVAL, DEFAULT_AI_CONNECTION_RETRY_INTERVAL);
  }

  public int getAiConnectionMaxRetryAttempts() {
    return getInt(KEY_AI_CONNECTION_MAX_RETRY_ATTEMPTS, DEFAULT_AI_CONNECTION_MAX_RETRY_ATTEMPTS);
  }

  public int getAiPollingTimeout() {
    return getInt(KEY_AI_POLLING_TIMEOUT, DEFAULT_AI_POLLING_TIMEOUT);
  }

  public int getAiPollingInterval() {
    return getInt(KEY_AI_POLLING_INTERVAL, DEFAULT_AI_POLLING_INTERVAL);
  }

  public int getAiUploadedChunkSizeMb() {
    return getInt(KEY_AI_UPLOADED_CHUNK_SIZE_MB, DEFAULT_AI_UPLOADED_CHUNK_SIZE_MB);
  }

  public boolean getEnableMessageDebugging() {
    return getBoolean(KEY_ENABLE_MESSAGE_DEBUGGING, DEFAULT_ENABLE_MESSAGE_DEBUGGING);
  }

  public boolean getIgnoreOutdatedInlineComments() {
    return getBoolean(KEY_IGNORE_OUTDATED_INLINE_COMMENTS, DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS);
  }

  public List<String> getSelectiveLogLevelOverride() {
    return splitListIntoItems(
        KEY_SELECTIVE_LOG_LEVEL_OVERRIDE, DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE);
  }

  private String getDefaultLangChainModel() {
    return switch (getLcProvider()) {
      case GEMINI -> DEFAULT_GEMINI_AI_MODEL;
      case MOONSHOT -> DEFAULT_MOONSHOT_AI_MODEL;
      case OPENAI -> DEFAULT_AI_MODEL;
    };
  }

  private String getDefaultLangChainDomain() {
    return switch (getLcProvider()) {
      case GEMINI -> GEMINI_DOMAIN;
      case MOONSHOT -> MOONSHOT_DOMAIN;
      case OPENAI -> OPENAI_DOMAIN;
    };
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public boolean isDefinedKey(String key) {
    return isDefinedKey(this.getClass(), key);
  }

  public TreeMap<String, String> dumpConfigMap() {
    return dumpConfigMap(this.getClass());
  }
}
