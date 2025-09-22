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
        (core, setting) ->
            core.splitConfig(core.getString(setting.key(), defaultValue)));
  }

  private static ConfigurationSetting<List<String>> splitSettingRemoveDots(
      String key, String defaultValue) {
    List<String> defaultList = splitDefaultRemoveDots(defaultValue);
    return ConfigurationSetting.of(
        key,
        defaultList,
        (core, setting) ->
            core.splitConfigRemoveDots(core.getString(setting.key(), defaultValue)));
  }

  public static final ConfigurationSetting<AiBackends> AI_BACKEND =
      ConfigurationSetting.enumSetting(KEY_AI_BACKEND, DEFAULT_AI_BACKEND, AiBackends.class);

  public static final ConfigurationSetting<LangChainProviders> LC_PROVIDER =
      ConfigurationSetting.enumSetting(KEY_LC_PROVIDER, DEFAULT_LC_PROVIDER, LangChainProviders.class);

  public static final ConfigurationSetting<String> AI_TOKEN =
      ConfigurationSetting.requiredString(KEY_AI_TOKEN);

  public static final ConfigurationSetting<String> GERRIT_USERNAME =
      ConfigurationSetting.requiredString(KEY_GERRIT_USERNAME);

  public static final ConfigurationSetting<String> AI_DOMAIN =
      stringWithLangChainFallback(KEY_AI_DOMAIN, OPENAI_DOMAIN, Configuration::getDefaultLangChainDomain);

  public static final ConfigurationSetting<String> AI_MODEL =
      stringWithLangChainFallback(KEY_AI_MODEL, DEFAULT_AI_MODEL, Configuration::getDefaultLangChainModel);

  public static final ConfigurationSetting<Boolean> REVIEW_COMMIT_MESSAGES =
      ConfigurationSetting.bool(KEY_REVIEW_COMMIT_MESSAGES, DEFAULT_REVIEW_COMMIT_MESSAGES);

  public static final ConfigurationSetting<Boolean> REVIEW_PATCH_SET =
      ConfigurationSetting.bool(KEY_REVIEW_PATCH_SET, DEFAULT_REVIEW_PATCH_SET);

  public static final ConfigurationSetting<Boolean> FULL_FILE_REVIEW =
      ConfigurationSetting.bool(KEY_FULL_FILE_REVIEW, DEFAULT_FULL_FILE_REVIEW);

  public static final ConfigurationSetting<CodeContextPolicies> CODE_CONTEXT_POLICY =
      ConfigurationSetting.enumSetting(
          KEY_CODE_CONTEXT_POLICY, DEFAULT_CODE_CONTEXT_POLICY, CodeContextPolicies.class);

  public static final ConfigurationSetting<String> CODE_CONTEXT_ON_DEMAND_BASE_PATH =
      ConfigurationSetting.string(
          KEY_CODE_CONTEXT_ON_DEMAND_BASE_PATH, DEFAULT_CODE_CONTEXT_ON_DEMAND_BASE_PATH);

  public static final ConfigurationSetting<Boolean> PROJECT_ENABLE =
      ConfigurationSetting.projectBoolean(KEY_PROJECT_ENABLE, DEFAULT_PROJECT_ENABLE);

  public static final ConfigurationSetting<Boolean> GLOBAL_ENABLE =
      ConfigurationSetting.globalBoolean(KEY_GLOBAL_ENABLE, DEFAULT_GLOBAL_ENABLE);

  public static final ConfigurationSetting<List<String>> DISABLED_USERS =
      splitSetting(KEY_DISABLED_USERS, DEFAULT_DISABLED_USERS);

  public static final ConfigurationSetting<List<String>> ENABLED_USERS =
      splitSetting(KEY_ENABLED_USERS, DEFAULT_ENABLED_USERS);

  public static final ConfigurationSetting<List<String>> DISABLED_GROUPS =
      splitSetting(KEY_DISABLED_GROUPS, DEFAULT_DISABLED_GROUPS);

  public static final ConfigurationSetting<List<String>> ENABLED_GROUPS =
      splitSetting(KEY_ENABLED_GROUPS, DEFAULT_ENABLED_GROUPS);

  public static final ConfigurationSetting<List<String>> DISABLED_TOPIC_FILTER =
      splitSetting(KEY_DISABLED_TOPIC_FILTER, DEFAULT_DISABLED_TOPIC_FILTER);

  public static final ConfigurationSetting<List<String>> ENABLED_TOPIC_FILTER =
      splitSetting(KEY_ENABLED_TOPIC_FILTER, DEFAULT_ENABLED_TOPIC_FILTER);

  public static final ConfigurationSetting<String> ENABLED_PROJECTS =
      ConfigurationSetting.globalString(KEY_ENABLED_PROJECTS, DEFAULT_ENABLED_PROJECTS);

  public static final ConfigurationSetting<Integer> MAX_REVIEW_LINES =
      ConfigurationSetting.integer(KEY_MAX_REVIEW_LINES, DEFAULT_MAX_REVIEW_LINES);

  public static final ConfigurationSetting<List<String>> ENABLED_FILE_EXTENSIONS =
      splitSettingRemoveDots(KEY_ENABLED_FILE_EXTENSIONS, DEFAULT_ENABLED_FILE_EXTENSIONS);

  public static final ConfigurationSetting<List<String>> DIRECTIVES =
      ConfigurationSetting.stringList(KEY_DIRECTIVES, DEFAULT_DIRECTIVES);

  public static final ConfigurationSetting<Boolean> ENABLED_VOTING =
      ConfigurationSetting.bool(KEY_ENABLED_VOTING, DEFAULT_ENABLED_VOTING);

  public static final ConfigurationSetting<Boolean> FILTER_NEGATIVE_COMMENTS =
      ConfigurationSetting.bool(KEY_FILTER_NEGATIVE_COMMENTS, DEFAULT_FILTER_NEGATIVE_COMMENTS);

  public static final ConfigurationSetting<Integer> FILTER_COMMENTS_BELOW_SCORE =
      ConfigurationSetting.integer(
          KEY_FILTER_COMMENTS_BELOW_SCORE, DEFAULT_FILTER_COMMENTS_BELOW_SCORE);

  public static final ConfigurationSetting<Boolean> FILTER_RELEVANT_COMMENTS =
      ConfigurationSetting.bool(KEY_FILTER_RELEVANT_COMMENTS, DEFAULT_FILTER_RELEVANT_COMMENTS);

  public static final ConfigurationSetting<Double> FILTER_COMMENTS_RELEVANCE_THRESHOLD =
      ConfigurationSetting.decimal(
          KEY_FILTER_COMMENTS_RELEVANCE_THRESHOLD, DEFAULT_FILTER_COMMENTS_RELEVANCE_THRESHOLD);

  public static final ConfigurationSetting<String> AI_RELEVANCE_RULES =
      ConfigurationSetting.string(KEY_AI_RELEVANCE_RULES, DEFAULT_EMPTY_SETTING);

  public static final ConfigurationSetting<String> AI_REVIEW_TEMPERATURE =
      ConfigurationSetting.string(
          KEY_AI_REVIEW_TEMPERATURE, String.valueOf(DEFAULT_AI_REVIEW_TEMPERATURE));

  public static final ConfigurationSetting<String> AI_COMMENT_TEMPERATURE =
      ConfigurationSetting.string(
          KEY_AI_COMMENT_TEMPERATURE, String.valueOf(DEFAULT_AI_COMMENT_TEMPERATURE));

  public static final ConfigurationSetting<Integer> VOTING_MIN_SCORE =
      ConfigurationSetting.integer(KEY_VOTING_MIN_SCORE, DEFAULT_VOTING_MIN_SCORE);

  public static final ConfigurationSetting<Integer> VOTING_MAX_SCORE =
      ConfigurationSetting.integer(KEY_VOTING_MAX_SCORE, DEFAULT_VOTING_MAX_SCORE);

  public static final ConfigurationSetting<Boolean> INLINE_COMMENTS_AS_RESOLVED =
      ConfigurationSetting.bool(
          KEY_INLINE_COMMENTS_AS_RESOLVED, DEFAULT_INLINE_COMMENTS_AS_RESOLVED);

  public static final ConfigurationSetting<Boolean> PATCH_SET_COMMENTS_AS_RESOLVED =
      ConfigurationSetting.bool(
          KEY_PATCH_SET_COMMENTS_AS_RESOLVED, DEFAULT_PATCH_SET_COMMENTS_AS_RESOLVED);

  public static final ConfigurationSetting<Boolean> IGNORE_OUTDATED_INLINE_COMMENTS =
      ConfigurationSetting.bool(
          KEY_IGNORE_OUTDATED_INLINE_COMMENTS, DEFAULT_IGNORE_OUTDATED_INLINE_COMMENTS);

  public static final ConfigurationSetting<Boolean> IGNORE_RESOLVED_AI_COMMENTS =
      ConfigurationSetting.bool(
          KEY_IGNORE_RESOLVED_AI_COMMENTS, DEFAULT_IGNORE_RESOLVED_AI_COMMENTS);

  public static final ConfigurationSetting<Boolean> FORCE_CREATE_ASSISTANT =
      ConfigurationSetting.bool(KEY_FORCE_CREATE_ASSISTANT, DEFAULT_FORCE_CREATE_ASSISTANT);

  public static final ConfigurationSetting<Boolean> TASK_SPECIFIC_ASSISTANTS =
      ConfigurationSetting.bool(KEY_TASK_SPECIFIC_ASSISTANTS, DEFAULT_TASK_SPECIFIC_ASSISTANTS);

  public static final ConfigurationSetting<Integer> AI_CONNECTION_TIMEOUT =
      ConfigurationSetting.integer(KEY_AI_CONNECTION_TIMEOUT, DEFAULT_AI_CONNECTION_TIMEOUT);

  public static final ConfigurationSetting<Integer> AI_CONNECTION_RETRY_INTERVAL =
      ConfigurationSetting.integer(
          KEY_AI_CONNECTION_RETRY_INTERVAL, DEFAULT_AI_CONNECTION_RETRY_INTERVAL);

  public static final ConfigurationSetting<Integer> AI_CONNECTION_MAX_RETRY_ATTEMPTS =
      ConfigurationSetting.integer(
          KEY_AI_CONNECTION_MAX_RETRY_ATTEMPTS, DEFAULT_AI_CONNECTION_MAX_RETRY_ATTEMPTS);

  public static final ConfigurationSetting<Integer> AI_POLLING_TIMEOUT =
      ConfigurationSetting.integer(KEY_AI_POLLING_TIMEOUT, DEFAULT_AI_POLLING_TIMEOUT);

  public static final ConfigurationSetting<Integer> AI_POLLING_INTERVAL =
      ConfigurationSetting.integer(KEY_AI_POLLING_INTERVAL, DEFAULT_AI_POLLING_INTERVAL);

  public static final ConfigurationSetting<Integer> AI_UPLOADED_CHUNK_SIZE_MB =
      ConfigurationSetting.integer(
          KEY_AI_UPLOADED_CHUNK_SIZE_MB, DEFAULT_AI_UPLOADED_CHUNK_SIZE_MB);

  public static final ConfigurationSetting<Boolean> ENABLE_MESSAGE_DEBUGGING =
      ConfigurationSetting.bool(KEY_ENABLE_MESSAGE_DEBUGGING, DEFAULT_ENABLE_MESSAGE_DEBUGGING);

  public static final ConfigurationSetting<Integer> LC_MAX_MEMORY_TOKENS =
      ConfigurationSetting.integer(KEY_LC_MAX_MEMORY_TOKENS, DEFAULT_LC_MAX_MEMORY_TOKENS);

  public static final ConfigurationSetting<List<String>> SELECTIVE_LOG_LEVEL_OVERRIDE =
      ConfigurationSetting.stringList(
          KEY_SELECTIVE_LOG_LEVEL_OVERRIDE, DEFAULT_SELECTIVE_LOG_LEVEL_OVERRIDE);

  public Configuration(
      OneOffRequestContext context,
      GerritApi gerritApi,
      PluginConfig globalConfig,
      PluginConfig projectConfig,
      String gerritUserEmail,
      Account.Id userId) {
    super(context, gerritApi, globalConfig, projectConfig, gerritUserEmail, userId);
  }

  // The default system prompt/instructions are specified in the prompt files and are passed as a
  // parameter
  public String aiSystemPromptInstructions(String defaultAiSystemPromptInstructions) {
    return getString(KEY_AI_SYSTEM_PROMPT_INSTRUCTIONS, defaultAiSystemPromptInstructions);
  }

  // If the default system prompt/instructions are not available in the caller's scope (e.g., when
  // displaying the configuration after a command request), they are retrieved from the prompt
  // files.
  public String aiSystemPromptInstructions() {
    Map<String, Object> systemPrompts = getJsonPromptValues("prompts");
    return aiSystemPromptInstructions(
        systemPrompts.get("DEFAULT_AI_SYSTEM_PROMPT_INSTRUCTIONS").toString());
  }

  private String getDefaultLangChainModel() {
    return switch (get(LC_PROVIDER)) {
      case GEMINI -> DEFAULT_GEMINI_AI_MODEL;
      case MOONSHOT -> DEFAULT_MOONSHOT_AI_MODEL;
      case OPENAI -> DEFAULT_AI_MODEL;
    };
  }

  private String getDefaultLangChainDomain() {
    return switch (get(LC_PROVIDER)) {
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
