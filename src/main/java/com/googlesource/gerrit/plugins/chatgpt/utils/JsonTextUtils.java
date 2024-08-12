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
    private static final Pattern JSON_COMPLEX_VALUE = Pattern.compile("^[\\[{].*[\\]}]$", Pattern.DOTALL);

    public static String unwrapJsonCode(String text) {
        return JSON_DELIMITED.matcher(text).replaceAll("$1");
    }

    public static boolean isJsonObjectAsString(String text) {
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
        log.debug("Processing entry: key={}, value=`{}`", key, value);

        return formatValue(key, value, "");
    }

    private static String formatValue(String key, String value, String indent) {
        JsonElement jsonElement = parseJsonWithDeSlash(value);
        if (jsonElement != null) {
            StringBuilder jsonString = new StringBuilder(indent + key + COLON_SPACE);
            if (jsonElement.isJsonObject()) {
                log.debug("Value is a valid JSON object; formatting JSON for key={}", key);
                jsonString.append(formatJsonObject(jsonElement.getAsJsonObject(), indent + INDENT));
            } else if (jsonElement.isJsonArray()) {
                log.debug("Value is a valid JSON array; formatting JSON array for key={}", key);
                jsonString.append(formatJsonArray(jsonElement.getAsJsonArray(), indent + INDENT));
            } else if (jsonElement.isJsonPrimitive()) {
                log.debug("Value for key {} is not JSON element: `{}`", key, value);
                jsonString.append(indent).append(jsonElement.getAsString());
            }
            return jsonString.toString();
        }
        return key + COLON_SPACE + value;
    }

    private static JsonElement parseJsonWithDeSlash(String str) {
        try {
            JsonElement element = JsonParser.parseString(str);
            if (!element.isJsonObject() && !element.isJsonArray() && JSON_COMPLEX_VALUE.matcher(str).matches()) {
                element = JsonParser.parseString(deSlashQuotes(str));
                log.debug("Detected potential Json element as String in `{}`", element);
            }
            return element;
        } catch (JsonSyntaxException e) {
            log.debug("String is not a valid JSON: {}", str);
            return null;
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
                    } else if (JSON_COMPLEX_VALUE.matcher(subValue.getAsString()).matches()) {
                        log.debug("Key={} contains a potential Json element as String", entry.getKey());
                        return formatValue(entry.getKey(), subValue.getAsString(), indent);
                    } else {
                        log.debug("Key={} contains a String", entry.getKey());
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
                        log.debug("Element=`{}` contains a nested JsonObject", element.getAsString());
                        return formatJsonObject(element.getAsJsonObject(), indent + INDENT);
                    } else if (element.isJsonArray()) {
                        log.debug("Element=`{}` contains a nested JsonArray", element.getAsString());
                        return formatJsonArray(element.getAsJsonArray(), indent + INDENT);
                    } else if (JSON_COMPLEX_VALUE.matcher(element.getAsString()).matches()) {
                        log.debug("Element`=`{}` contains a potential Json element as String", element.getAsString());
                        return formatValue("", element.getAsString(), indent);
                    } else {
                        log.debug("Element=`{}` contains a String", element.getAsString());
                        return indent + element.getAsString();
                    }
                })
                .collect(Collectors.toList()));
    }
}
