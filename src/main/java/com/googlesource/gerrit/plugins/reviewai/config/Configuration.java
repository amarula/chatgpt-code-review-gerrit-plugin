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
import com.googlesource.gerrit.plugins.reviewai.utils.TextUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

import static com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPrompt.getJsonPromptValues;
import static com.googlesource.gerrit.plugins.reviewai.aibackend.openai.client.code.context.CodeContextPolicyBase.CodeContextPolicies;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.AiBackends;
import static com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;

@Getter
@Accessors(fluent = true)
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

  private static final AiBackends DEFAULT_AI_BACKEND = AiBackends.OPENAI;
  private static final LangChainProviders DEFAULT_LC_PROVIDER = LangChainProviders.OPENAI;
  private static final boolean DEFAULT_REVIEW_PATCH_SET = true;
  private static final boolean DEFAULT_REVIEW_COMMIT_MESSAGES = true;
  private static final boolean DEFAULT_FULL_FILE_REVIEW = true;
  private static final CodeContextPolicies DEFAULT_CODE_CONTEXT_POLICY =
      CodeContextPolicies.UPLOAD_ALL;
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

  private static List<String> splitDefault(String value) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    return List.copyOf(TextUtils.splitString(value));
  }

  private static List<String> splitDefaultRemoveDots(String value) {
    return splitDefault(value).stream().map(s -> s.replaceFirst("^\\.", "")).toList();
  }

  private static ConfigurationSetting<List<String>> splitSetting(String key, String defaultValue) {
    List<String> defaultList = splitDefault(defaultValue);
    return ConfigurationSetting.of(
        key,
        defaultList,
        (core, setting) -> core.splitConfig(core.getString(setting.key(), defaultValue)));
  }

  private static ConfigurationSetting<List<String>> splitSettingRemoveDots(
      String key, String defaultValue) {
    List<String> defaultList = splitDefaultRemoveDots(defaultValue);
    return ConfigurationSetting.of(
        key,
        defaultList,
        (core, setting) -> core.splitConfigRemoveDots(core.getString(setting.key(), defaultValue)));
  }

  private final AiBackends aiBackend;
  private final LangChainProviders lcProvider;
  private final String aiToken;
  private final String gerritUserName;
  private final String aiDomain;
  private final String aiModel;
  private final boolean aiReviewCommitMessages;
  private final boolean aiReviewPatchSet;
  private final boolean aiFullFileReview;
  private final CodeContextPolicies codeContextPolicy;
  private final String codeContextOnDemandBasePath;
  private final boolean projectEnable;
  private final boolean globalEnable;
  private final List<String> disabledUsers;
  private final List<String> enabledUsers;
  private final List<String> disabledGroups;
  private final List<String> enabledGroups;
  private final List<String> disabledTopicFilter;
  private final List<String> enabledTopicFilter;
  private final String enabledProjects;
  private final int maxReviewLines;
  private final List<String> enabledFileExtensions;
  private final List<String> directives;
  private final boolean enabledVoting;
  private final boolean filterNegativeComments;
  private final int filterCommentsBelowScore;
  private final boolean filterRelevantComments;
  private final double filterCommentsRelevanceThreshold;
  private final String aiRelevanceRules;
  private final String aiReviewTemperature;
  private final String aiCommentTemperature;
  private final int votingMinScore;
  private final int votingMaxScore;
  private final boolean inlineCommentsAsResolved;
  private final boolean patchSetCommentsAsResolved;
  private final boolean ignoreOutdatedInlineComments;
  private final boolean ignoreResolvedAiComments;
  private final boolean forceCreateAssistant;
  private final boolean taskSpecificAssistants;
  private final int aiConnectionTimeout;
  private final int aiConnectionRetryInterval;
  private final int aiConnectionMaxRetryAttempts;
  private final int aiPollingTimeout;
  private final int aiPollingInterval;
  private final int aiUploadedChunkSizeMb;
  private final boolean enableMessageDebugging;
  private final int lcMaxMemoryTokens;
  private final List<String> selectiveLogLevelOverride;
  private final String aiSystemPromptInstructions;

  public Configuration(
      OneOffRequestContext context,
      GerritApi gerritApi,
      PluginConfig globalConfig,
      PluginConfig projectConfig,
      String gerritUserEmail,
      Account.Id userId) {
    super(context, gerritApi, globalConfig, projectConfig, gerritUserEmail, userId);
    aiBackend =
        get(ConfigurationSetting.enumSetting(KEY_AI_BACKEND, DEFAULT_AI_BACKEND, AiBackends.class));
    lcProvider =
        get(ConfigurationSetting.enumSetting(
                KEY_LC_PROVIDER, DEFAULT_LC_PROVIDER, LangChainProviders.class));
    aiToken = get(ConfigurationSetting.requiredString(KEY_AI_TOKEN));
    gerritUserName = get(ConfigurationSetting.requiredString(KEY_GERRIT_USERNAME));
    aiDomain = resolveAiDomain();
    aiModel = resolveAiModel();
    aiReviewCommitMessages =
        get(ConfigurationSetting.bool(KEY_REVIEW_COMMIT_MESSAGES, DEFAULT_REVIEW_COMMIT_MESSAGES));
    aiReviewPatchSet =
        get(ConfigurationSetting.bool(KEY_REVIEW_PATCH_SET, DEFAULT_REVIEW_PATCH_SET));
    aiFullFileReview =
        get(ConfigurationSetting.bool(KEY_FULL_FILE_REVIEW, DEFAULT_FULL_FILE_REVIEW));
    codeContextPolicy =
        get(ConfigurationSetting.enumSetting(
                KEY_CODE_CONTEXT_POLICY, DEFAULT_CODE_CONTEXT_POLICY, CodeContextPolicies.class));
    codeContextOnDemandBasePath =
        get(ConfigurationSetting.string(
                KEY_CODE_CONTEXT_ON_DEMAND_BASE_PATH, DEFAULT_CODE_CONTEXT_ON_DEMAND_BASE_PATH));
    projectEnable =
        get(ConfigurationSetting.projectBoolean(KEY_PROJECT_ENABLE, DEFAULT_PROJECT_ENABLE));
    globalEnable =
        get(ConfigurationSetting.globalBoolean(KEY_GLOBAL_ENABLE, DEFAULT_GLOBAL_ENABLE));
    disabledUsers = List.copyOf(get(splitSetting(KEY_DISABLED_USERS, DEFAULT_DISABLED_USERS)));
    enabledUsers = List.copyOf(get(splitSetting(KEY_ENABLED_USERS, DEFAULT_ENABLED_USERS)));
    disabledGroups = List.copyOf(get(splitSetting(KEY_DISABLED_GROUPS, DEFAULT_DISABLED_GROUPS)));
    enabledGroups = List.copyOf(get(splitSetting(KEY_ENABLED_GROUPS, DEFAULT_ENABLED_GROUPS)));
    disabledTopicFilter =
        List.copyOf(get(splitSetting(KEY_DISABLED_TOPIC_FILTER, DEFAULT_DISABLED_TOPIC_FILTER)));
    enabledTopicFilter =
        List.copyOf(get(splitSetting(KEY_ENABLED_TOPIC_FILTER, DEFAULT_ENABLED_TOPIC_FILTER)));
    enabledProjects =
        get(ConfigurationSetting.globalString(KEY_ENABLED_PROJECTS, DEFAULT_ENABLED_PROJECTS));
    maxReviewLines =
        get(ConfigurationSetting.integer(KEY_MAX_REVIEW_LINES, DEFAULT_MAX_REVIEW_LINES));
    enabledFileExtensions =
        List.copyOf(
            get(splitSettingRemoveDots(
                    KEY_ENABLED_FILE_EXTENSIONS, DEFAULT_ENABLED_FILE_EXTENSIONS)));
    directives =
        List.copyOf(get(ConfigurationSetting.stringList(KEY_DIRECTIVES, DEFAULT_DIRECTIVES)));
    enabledVoting = get(ConfigurationSetting.bool(KEY_ENABLED_VOTING, DEFAULT_ENABLED_VOTING));
    filterNegativeComments =
        get(ConfigurationSetting.bool(
                KEY_FILTER_NEGATIVE_COMMENTS, DEFAULT_FILTER_NEGATIVE_COMMENTS));
    filterCommentsBelowScore =
        get(ConfigurationSetting.integer(
                KEY_FILTER_COMMENTS_BELOW_SCORE, DEFAULT_FILTER_COMMENTS_BELOW_SCORE));
    filterRelevantComments =
        get(ConfigurationSetting.bool(
                KEY_FILTER_RELEVANT_COMMENTS, DEFAULT_FILTER_RELEVANT_COMMENTS));
    filterCommentsRelevanceThreshold =
        get(ConfigurationSetting.decimal(
                KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD,
                DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD));
    aiRelevanceRules =
        get(ConfigurationSetting.string(KEY_AI_RELEVANCE_RULES, DEFAULT_EMPTY_SETTING));
    aiReviewTemperature =
        get(ConfigurationSetting.string(
                KEY_AI_REVIEW_TEMPERATURE, String.valueOf(DEFAULT_AI_REVIEW_TEMPERATURE)));
    aiCommentTemperature =
        get(ConfigurationSetting.string(
                KEY_AI_COMMENT_TEMPERATURE, String.valueOf(DEFAULT_AI_COMMENT_TEMPERATURE)));
    votingMinScore =
        get(ConfigurationSetting.integer(KEY_VOTING_MIN_SCORE, DEFAULT_VOTING_MIN_SCORE));
    votingMaxScore =
        get(ConfigurationSetting.integer(KEY_VOTING_MAX_SCORE, DEFAULT_VOTING_MAX_SCORE));
    inlineCommentsAsResolved =
        get(ConfigurationSetting.bool(
                KEY_INLINE_COMMENTS_AS_RESOLVED, DEFAULT_INLINE_COMMENTS_AS_RESOLVED));
    patchSetCommentsAsResolved =
        get(ConfigurationSetting.bool(
                KEY_PATCH_SET_COMMENTS_AS_RESOLVED, DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED));
    ignoreOutdatedInlineComments =
        get(ConfigurationSetting.bool(
                KEY_IGNORE_OUTDATED_INLINE_COMMENTS, DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS));
    ignoreResolvedAiComments =
        get(ConfigurationSetting.bool(
                KEY_IGNORE_RESOLVED_AI_COMMENTS, DEFAULT_IGNORE_RESOLVED_AI_COMMENTS));
    forceCreateAssistant =
        get(ConfigurationSetting.bool(KEY_FORCE_CREATE_ASSISTANT, DEFAULT_FORCE_CREATE_ASSISTANT));
    taskSpecificAssistants =
        get(ConfigurationSetting.bool(
                KEY_TASK_SPECIFIC_ASSISTANTS, DEFAULT_TASK_SPECIFIC_ASSISTANTS));
    aiConnectionTimeout =
        get(ConfigurationSetting.integer(KEY_AI_CONNECTION_TIMEOUT, DEFAULT_AI_CONNECTION_TIMEOUT));
    aiConnectionRetryInterval =
        get(ConfigurationSetting.integer(
                KEY_AI_CONNECTION_RETRY_INTERVAL, DEFAULT_AI_CONNECTION_RETRY_INTERVAL));
    aiConnectionMaxRetryAttempts =
        get(ConfigurationSetting.integer(
                KEY_AI_CONNECTION_MAX_RETRY_ATTEMPTS, DEFAULT_AI_CONNECTION_MAX_RETRY_ATTEMPTS));
    aiPollingTimeout =
        get(ConfigurationSetting.integer(KEY_AI_POLLING_TIMEOUT, DEFAULT_AI_POLLING_TIMEOUT));
    aiPollingInterval =
        get(ConfigurationSetting.integer(KEY_AI_POLLING_INTERVAL, DEFAULT_AI_POLLING_INTERVAL));
    aiUploadedChunkSizeMb =
        get(ConfigurationSetting.integer(
                KEY_AI_UPLOADED_CHUNK_SIZE_MB, DEFAULT_AI_UPLOADED_CHUNK_SIZE_MB));
    enableMessageDebugging =
        get(ConfigurationSetting.bool(
                KEY_ENABLE_MESSAGE_DEBUGGING, DEFAULT_ENABLE_MESSAGE_DEBUGGING));
    lcMaxMemoryTokens =
        get(ConfigurationSetting.integer(KEY_LC_MAX_MEMORY_TOKENS, DEFAULT_LC_MAX_MEMORY_TOKENS));
    selectiveLogLevelOverride =
        List.copyOf(
            get(ConfigurationSetting.stringList(
                    KEY_SELECTIVE_LOG_LEVEL_OVERRIDE, DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE)));
    aiSystemPromptInstructions = loadAiSystemPromptInstructions();
  }

  private String resolveAiDomain() {
    String configured = getString(KEY_AI_DOMAIN);
    if (configured != null && !configured.isEmpty()) {
      return configured;
    }
    return aiBackend == AiBackends.LANGCHAIN ? getDefaultLangChainDomain() : OPENAI_DOMAIN;
  }

  private String resolveAiModel() {
    String configured = getString(KEY_AI_MODEL);
    if (configured != null && !configured.isEmpty()) {
      return configured;
    }
    return aiBackend == AiBackends.LANGCHAIN ? getDefaultLangChainModel() : DEFAULT_AI_MODEL;
  }

  // The default system prompt/instructions are specified in the prompt files and are passed as a
  // parameter
  public String aiSystemPromptInstructions(String defaultAiSystemPromptInstructions) {
    return getString(KEY_AI_SYSTEM_PROMPT_INSTRUCTIONS, defaultAiSystemPromptInstructions);
  }

  private String loadAiSystemPromptInstructions() {
    Map<String, Object> systemPrompts = getJsonPromptValues("prompts");
    return aiSystemPromptInstructions(
        systemPrompts.get("DEFAULT_AI_SYSTEM_PROMPT_INSTRUCTIONS").toString());
  }

  private String getDefaultLangChainModel() {
    return switch (lcProvider) {
      case GEMINI -> DEFAULT_GEMINI_AI_MODEL;
      case MOONSHOT -> DEFAULT_MOONSHOT_AI_MODEL;
      case OPENAI -> DEFAULT_AI_MODEL;
    };
  }

  private String getDefaultLangChainDomain() {
    return switch (lcProvider) {
      case GEMINI -> GEMINI_DOMAIN;
      case MOONSHOT -> MOONSHOT_DOMAIN;
      case OPENAI -> OPENAI_DOMAIN;
    };
  }

  public boolean isDefinedKey(String key) {
    return isDefinedKey(this.getClass(), key);
  }

  public TreeMap<String, String> dumpConfigMap() {
    return dumpConfigMap(this.getClass());
  }
}
