package com.googlesource.gerrit.plugins.chatgpt.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class JsonTextUtils extends TextUtils {
    private static final String INDENT = "    ";
    private static final Pattern JSON_DELIMITED = Pattern.compile("^.*?" + CODE_DELIMITER + "json\\s*(.*)\\s*" +
                    CODE_DELIMITER + ".*$", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("^\\{.*\\}$", Pattern.DOTALL);

    public static String unwrapJsonCode(String text) {
        return JSON_DELIMITED.matcher(text).replaceAll("$1");
    }

    public static boolean isJsonString(String text) {
        return JSON_OBJECT.matcher(text).matches() || JSON_DELIMITED.matcher(text).matches();
    }

    public static String prettyStringifyMap(Map<String, String> map) {
        log.info("Starting to pretty stringify map: {}", map);
        return joinWithNewLine(
                map.entrySet().stream()
                        .map(JsonTextUtils::formatEntry)
                        .collect(Collectors.toList())
        );
    }

    private static String formatEntry(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        String value = entry.getValue();
        log.debug("Processing entry: key={}, value={}", key, value);

        if (isJson(value)) {
            try {
                JsonElement jsonElement = JsonParser.parseString(value);
                StringBuilder jsonString = new StringBuilder(key + COLON_SPACE);
                if (jsonElement.isJsonObject()) {
                    log.debug("Value is a valid JSON object; formatting JSON for key={}", key);
                    jsonString.append(formatJsonObject(jsonElement.getAsJsonObject(), INDENT));
                } else if (jsonElement.isJsonArray()) {
                    log.debug("Value is a valid JSON array; formatting JSON array for key={}", key);
                    jsonString.append(formatJsonArray(jsonElement.getAsJsonArray(), INDENT));
                } else {
                    log.error("Value for key {} is not a valid JSON object nor array: {}", key, value);
                }
                return jsonString.toString();
            } catch (JsonSyntaxException e) {
                log.error("Error parsing JSON for key={}, value={}, error={}", key, value, e.getMessage());
            }
        }
        return key + COLON_SPACE + value;
    }

    private static boolean isJson(String str) {
        try {
            JsonElement element = JsonParser.parseString(str);
            return element.isJsonObject() || element.isJsonArray();
        } catch (JsonSyntaxException e) {
            log.error("String is not a valid JSON: {}", str);
            return false;
        }
    }

    private static String formatJsonObject(JsonObject jsonObject, String indent) {
        log.debug("Formatting JsonObject: {}", jsonObject);
        if (jsonObject.size() == 0) {
            return "";
        }
        return "\n" + joinWithNewLine(jsonObject.entrySet().stream()
                .map(entry -> {
                    JsonElement subValue = entry.getValue();
                    StringBuilder objBuilder = new StringBuilder(indent + entry.getKey() + COLON_SPACE);
                    if (subValue.isJsonObject()) {
                        log.debug("Key={} contains a nested JsonObject", entry.getKey());
                        objBuilder.append(formatJsonObject(subValue.getAsJsonObject(), indent + INDENT));
                    } else if (subValue.isJsonArray()) {
                        log.debug("Key={} contains a nested JsonArray", entry.getKey());
                        objBuilder.append(formatJsonArray(subValue.getAsJsonArray(), indent + INDENT));
                    } else {
                        objBuilder.append(subValue.getAsString());
                    }
                    return objBuilder.toString();
                })
                .collect(Collectors.toList()));
    }

    private static String formatJsonArray(JsonArray jsonArray, String indent) {
        log.debug("Formatting JsonArray: {}", jsonArray);
        if (jsonArray.isEmpty()) {
            return "";
        }
        return "\n" + joinWithNewLine(StreamSupport.stream(jsonArray.spliterator(), false)
                .map(element -> {
                    if (element.isJsonObject()) {
                        return formatJsonObject(element.getAsJsonObject(), indent + INDENT);
                    } else if (element.isJsonArray()) {
                        return formatJsonArray(element.getAsJsonArray(), indent + INDENT);
                    } else {
                        return indent + element.getAsString();
                    }
                })
                .collect(Collectors.toList()));
    }
}
