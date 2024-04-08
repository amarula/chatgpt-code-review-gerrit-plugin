package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.prompt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.data.ChangeSetDataHandler;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.chatgpt.utils.FileUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

@Slf4j
public class ChatGptPrompt {
    public static final String SPACE = " ";
    public static final String DOT = ". ";

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
    public static String DEFAULT_GPT_SYSTEM_PROMPT;
    public static String DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION;
    public static String DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW;
    public static String DEFAULT_GPT_REVIEW_PROMPT;
    public static String DEFAULT_GPT_REVIEW_PROMPT_REVIEW;
    public static String DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY;
    public static String DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES;
    public static String DEFAULT_GPT_REVIEW_PROMPT_DIFF;
    public static String DEFAULT_GPT_REPLIES_PROMPT;
    public static String DEFAULT_GPT_REPLIES_PROMPT_INLINE;
    public static String DEFAULT_GPT_REPLIES_PROMPT_ENFORCE_RESPONSE_CHECK;
    public static String DEFAULT_GPT_REQUEST_PROMPT_DIFF;
    public static String DEFAULT_GPT_REQUEST_PROMPT_REQUESTS;
    public static String DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES;
    public static String DEFAULT_GPT_RELEVANCE_RULES;
    public static Map<String, String> DEFAULT_GPT_REPLIES_ATTRIBUTES;

    private final Configuration config;
    @Setter
    private boolean isCommentEvent;

    public ChatGptPrompt(Configuration config) {
        this.config = config;
        loadPrompts();
    }

    public ChatGptPrompt(Configuration config, boolean isCommentEvent) {
        this(config);
        this.isCommentEvent = isCommentEvent;
    }

    public static String getDefaultGptReviewSystemPrompt() {
        return DEFAULT_GPT_SYSTEM_PROMPT + DOT +
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION + SPACE +
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW;
    }

    public static String getCommentRequestUserPrompt(int commentPropertiesSize) {
        return buildFieldSpecifications(REQUEST_REPLY_ATTRIBUTES) + SPACE +
                DEFAULT_GPT_REPLIES_PROMPT_INLINE + SPACE +
                String.format(DEFAULT_GPT_REPLIES_PROMPT_ENFORCE_RESPONSE_CHECK, commentPropertiesSize);
    }

    public String getPatchSetReviewUserPrompt() {
        List<String> attributes = new ArrayList<>(PATCH_SET_REVIEW_REPLY_ATTRIBUTES);
        if (config.isVotingEnabled() || config.getFilterNegativeComments()) {
            updateScoreDescription();
        }
        else {
            attributes.remove(ATTRIBUTE_SCORE);
        }
        updateRelevanceDescription();
        return buildFieldSpecifications(attributes) + SPACE +
                DEFAULT_GPT_REPLIES_PROMPT_INLINE;
    }

    public String getGptSystemPrompt() {
        List<String> prompt = new ArrayList<>(Arrays.asList(
                config.getString(Configuration.KEY_GPT_SYSTEM_PROMPT, DEFAULT_GPT_SYSTEM_PROMPT), DOT,
                DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION
        ));
        if (!isCommentEvent) {
            prompt.addAll(Arrays.asList(SPACE, DEFAULT_GPT_SYSTEM_PROMPT_INPUT_DESCRIPTION_REVIEW));
        }
        return concatenate(prompt);
    }

    public String getGptUserPrompt(String patchSet, String changeId) {
        List<String> prompt = new ArrayList<>();
        ChangeSetData changeSetData = ChangeSetDataHandler.getInstance(changeId);
        String gptRequestUserPrompt = changeSetData.getGptRequestUserPrompt();
        boolean isValidRequestUserPrompt = gptRequestUserPrompt != null && !gptRequestUserPrompt.isEmpty();
        if (isCommentEvent && isValidRequestUserPrompt) {
            log.debug("ConfigsDynamically value found: {}", gptRequestUserPrompt);
            prompt.addAll(Arrays.asList(
                    DEFAULT_GPT_REQUEST_PROMPT_DIFF,
                    patchSet,
                    DEFAULT_GPT_REQUEST_PROMPT_REQUESTS,
                    gptRequestUserPrompt,
                    getCommentRequestUserPrompt(changeSetData.getCommentPropertiesSize())
            ));
        }
        else {
            prompt.add(DEFAULT_GPT_REVIEW_PROMPT);
            prompt.addAll(getReviewSteps());
            prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIFF);
            prompt.add(patchSet);
            if (isValidRequestUserPrompt) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_MESSAGE_HISTORY);
                prompt.add(gptRequestUserPrompt);
            }
            if (!changeSetData.getDirectives().isEmpty()) {
                prompt.add(DEFAULT_GPT_REVIEW_PROMPT_DIRECTIVES);
                prompt.add(getNumberedListString(new ArrayList<>(changeSetData.getDirectives())));
            }
        }
        return joinWithNewLine(prompt);
    }

    private static String buildFieldSpecifications(List<String> filterFields) {
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

        return String.format(DEFAULT_GPT_REPLIES_PROMPT,
                joinWithComma(attributes.keySet()),
                joinWithSemicolon(fieldDescription)
        );
    }

    private List<String> getReviewSteps() {
        List<String> steps = new ArrayList<>(){};
        steps.add(DEFAULT_GPT_REVIEW_PROMPT_REVIEW + SPACE + getPatchSetReviewUserPrompt());
        if (config.getGptReviewCommitMessages()) {
            steps.add(DEFAULT_GPT_REVIEW_PROMPT_COMMIT_MESSAGES);
        }
        return steps;
    }

    private void updateScoreDescription() {
        String scoreDescription = DEFAULT_GPT_REPLIES_ATTRIBUTES.get(ATTRIBUTE_SCORE);
        if (scoreDescription.contains("%d")) {
            scoreDescription = String.format(scoreDescription, config.getVotingMinScore(), config.getVotingMaxScore());
            DEFAULT_GPT_REPLIES_ATTRIBUTES.put(ATTRIBUTE_SCORE, scoreDescription);
        }
    }

    private void updateRelevanceDescription() {
        String relevanceDescription = DEFAULT_GPT_REPLIES_ATTRIBUTES.get(ATTRIBUTE_RELEVANCE);
        if (relevanceDescription.contains("%s")) {
            String defaultGptRelevanceRules = config.getString(Configuration.KEY_GPT_RELEVANCE_RULES,
                    DEFAULT_GPT_RELEVANCE_RULES);
            relevanceDescription = String.format(relevanceDescription, defaultGptRelevanceRules);
            DEFAULT_GPT_REPLIES_ATTRIBUTES.put(ATTRIBUTE_RELEVANCE, relevanceDescription);
        }
    }

    private void loadPrompts() {
        // Avoid repeated loading of prompt constants
        if (DEFAULT_GPT_SYSTEM_PROMPT != null) return;
        Gson gson = new Gson();
        Class<? extends ChatGptPrompt> me = this.getClass();
        try (InputStreamReader reader = FileUtils.getInputStreamReader("Config/prompts.json")) {
            Map<String, Object> values = gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                try {
                    Field field = me.getDeclaredField(entry.getKey());
                    field.setAccessible(true);
                    field.set(null, entry.getValue());
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error("Error setting prompt '{}'", entry.getKey(), e);
                    throw new IOException();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompts", e);
        }
        // Keep the given order of attributes
        DEFAULT_GPT_REPLIES_ATTRIBUTES = new LinkedHashMap<>(DEFAULT_GPT_REPLIES_ATTRIBUTES);
    }

}
