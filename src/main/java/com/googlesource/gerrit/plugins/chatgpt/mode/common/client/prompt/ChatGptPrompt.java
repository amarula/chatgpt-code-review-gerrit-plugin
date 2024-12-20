package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.google.gson.reflect.TypeToken;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.GsonUtils.getGson;
import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public class ChatGptPrompt {
    // Reply attributes
    public static final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_REPLY = "reply";
    public static final String ATTRIBUTE_SCORE = "score";
    public static final String ATTRIBUTE_REPEATED = "repeated";
    public static final String ATTRIBUTE_CONFLICTING = "conflicting";
    public static final String ATTRIBUTE_RELEVANCE = "relevance";
    public static final String ATTRIBUTE_CHANGE_ID = "changeId";
    public static final List<String> PATCH_SET_REVIEW_REPLY_ATTRIBUTES = new ArrayList<>(Arrays.asList(
            ATTRIBUTE_REPLY, ATTRIBUTE_SCORE, ATTRIBUTE_REPEATED, ATTRIBUTE_CONFLICTING, ATTRIBUTE_RELEVANCE
    ));
    public static final List<String> REQUEST_REPLY_ATTRIBUTES = new ArrayList<>(Arrays.asList(
            ATTRIBUTE_REPLY, ATTRIBUTE_ID, ATTRIBUTE_CHANGE_ID
    ));

    // Prompt constants loaded from JSON file
    public static String DEFAULT_GPT_SYSTEM_PROMPT_INSTRUCTIONS;
    public static String DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES;
    public static String DEFAULT_GPT_PROMPT_FORCE_JSON_FORMAT;
    public static String DEFAULT_GPT_REPLIES_PROMPT_SPECS;
    public static String DEFAULT_GPT_REPLIES_PROMPT_INLINE;
    public static String DEFAULT_GPT_REPLIES_PROMPT_ENFORCE_RESPONSE_CHECK;
    public static String DEFAULT_GPT_REQUEST_PROMPT_DIFF;
    public static String DEFAULT_GPT_REQUEST_PROMPT_REQUESTS;
    public static String DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES;
    public static String DEFAULT_GPT_REVIEW_PROMPT_INSTRUCTIONS_COMMIT_MESSAGES;
    public static String DEFAULT_GPT_RELEVANCE_RULES;
    public static String DEFAULT_GPT_HOW_TO_FIND_COMMIT_MESSAGE;
    public static Map<String, String> DEFAULT_GPT_REPLIES_ATTRIBUTES;

    protected final Configuration config;

    @Setter
    protected boolean isCommentEvent;

    public ChatGptPrompt(Configuration config) {
        this.config = config;
        loadDefaultPrompts("prompts");
        log.debug("ChatGptPrompt initialized.");
    }

    public ChatGptPrompt(Configuration config, boolean isCommentEvent) {
        this(config);
        this.isCommentEvent = isCommentEvent;
        log.debug("ChatGptPrompt initialized with isCommentEvent: {}", isCommentEvent);
    }

    public static String getReviewPromptCommitMessages() {
        log.debug("Constructing review prompt for commit messages.");
        return joinWithSpace(new ArrayList<>(List.of(
                String.format(DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES, DEFAULT_GPT_HOW_TO_FIND_COMMIT_MESSAGE),
                DEFAULT_GPT_REVIEW_PROMPT_INSTRUCTIONS_COMMIT_MESSAGES
        )));
    }

    public static Map<String, Object> getJsonPromptValues(String promptFilename) {
        String promptFile = String.format("config/%s.json", promptFilename);
        try (InputStreamReader reader = FileUtils.getInputStreamReader(promptFile)) {
            return getGson().fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (IOException e) {
            log.error("Failed to load prompts from file: {}", promptFilename, e);
            throw new RuntimeException("Failed to load prompts", e);
        }
    }

    protected void loadDefaultPrompts(String promptFilename) {
        Class<? extends ChatGptPrompt> me = this.getClass();
        Map<String, Object> values = getJsonPromptValues(promptFilename);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                Field field = me.getField(entry.getKey());
                field.setAccessible(true);
                field.set(null, entry.getValue());
                log.debug("Loaded prompt attribute: {} with value: {}", entry.getKey(), entry.getValue());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                log.error("Error setting prompt '{}'", entry.getKey(), e);
                throw new RuntimeException("Error setting prompt field", e);
            }
        }
        // Keep the given order of attributes
        DEFAULT_GPT_REPLIES_ATTRIBUTES = new LinkedHashMap<>(DEFAULT_GPT_REPLIES_ATTRIBUTES);
    }

    protected static String buildFieldSpecifications(List<String> filterFields) {
        log.debug("Building field specifications for filter fields: {}", filterFields);
        Set<String> orderedFilterFields = new LinkedHashSet<>(filterFields);
        Map<String, String> attributes = DEFAULT_GPT_REPLIES_ATTRIBUTES.entrySet().stream()
                .filter(entry -> orderedFilterFields.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> INLINE_CODE_DELIMITER + entry.getKey() + INLINE_CODE_DELIMITER,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        List<String> fieldDescription = attributes.entrySet().stream()
                .map(entry -> entry.getKey() + SPACE + entry.getValue())
                .collect(Collectors.toList());

        return String.format(DEFAULT_GPT_REPLIES_PROMPT_SPECS,
                joinWithComma(attributes.keySet()),
                joinWithSemicolon(fieldDescription)
        );
    }

    public String getPatchSetReviewPromptInstructions() {
        log.debug("Getting patch set review prompt instructions.");
        List<String> attributes = new ArrayList<>(PATCH_SET_REVIEW_REPLY_ATTRIBUTES);
        if (config.isVotingEnabled() || config.getFilterNegativeComments()) {
            updateScoreDescription();
        }
        else {
            attributes.remove(ATTRIBUTE_SCORE);
        }
        updateRelevanceDescription();
        return buildFieldSpecifications(attributes);
    }

    public String getPatchSetReviewPrompt() {
        log.debug("Getting patch set review prompt.");
        return getPatchSetReviewPromptInstructions() + SPACE + DEFAULT_GPT_REPLIES_PROMPT_INLINE;
    }

    private void updateScoreDescription() {
        log.debug("Updating score description.");
        String scoreDescription = DEFAULT_GPT_REPLIES_ATTRIBUTES.get(ATTRIBUTE_SCORE);
        if (scoreDescription.contains("%d")) {
            scoreDescription = String.format(scoreDescription, config.getVotingMinScore(), config.getVotingMaxScore());
            DEFAULT_GPT_REPLIES_ATTRIBUTES.put(ATTRIBUTE_SCORE, scoreDescription);
            log.debug("Updated score description to: {}", scoreDescription);
        }
    }

    private void updateRelevanceDescription() {
        log.debug("Updating relevance description.");
        String relevanceDescription = DEFAULT_GPT_REPLIES_ATTRIBUTES.get(ATTRIBUTE_RELEVANCE);
        if (relevanceDescription.contains("%s")) {
            String defaultGptRelevanceRules = config.getString(Configuration.KEY_GPT_RELEVANCE_RULES,
                    DEFAULT_GPT_RELEVANCE_RULES);
            relevanceDescription = String.format(relevanceDescription, defaultGptRelevanceRules);
            DEFAULT_GPT_REPLIES_ATTRIBUTES.put(ATTRIBUTE_RELEVANCE, relevanceDescription);
            log.debug("Updated relevance description to: {}", relevanceDescription);
        }
    }
}
