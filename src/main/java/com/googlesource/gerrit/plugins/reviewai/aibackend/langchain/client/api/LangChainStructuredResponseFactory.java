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

package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.client.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class LangChainStructuredResponseFactory {

  private final String schemaResourcePath;

  ResponseFormat loadStructuredResponseFormat() {
    try (InputStream inputStream =
        LangChainStructuredResponseFactory.class
            .getClassLoader()
            .getResourceAsStream(schemaResourcePath)) {
      if (inputStream == null) {
        log.warn(
            "Structured output schema resource {} not found; falling back to free-form text",
            schemaResourcePath);
        return null;
      }

      JsonObject root =
          JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      JsonObject function = root.getAsJsonObject("function");
      if (function == null) {
        log.warn(
            "Structured output schema resource {} missing 'function' definition; ignoring",
            schemaResourcePath);
        return null;
      }

      JsonElement nameElement = function.get("name");
      JsonElement parametersElement = function.get("parameters");
      if (nameElement == null || !nameElement.isJsonPrimitive()) {
        log.warn(
            "Structured output schema resource {} missing function name; ignoring",
            schemaResourcePath);
        return null;
      }
      if (parametersElement == null || parametersElement.isJsonNull()) {
        log.warn(
            "Structured output schema resource {} missing function parameters; ignoring",
            schemaResourcePath);
        return null;
      }

      JsonSchemaElement rootElement = null;
      if (parametersElement.isJsonObject()) {
        try {
          rootElement = buildJsonSchemaElement(parametersElement.getAsJsonObject());
        } catch (Exception e) {
          log.warn(
              "Failed to convert structured output schema {} into LangChain schema classes",
              schemaResourcePath,
              e);
        }
      }

      if (rootElement == null) {
        log.warn(
            "Structured output schema {} could not be converted; structured responses disabled",
            schemaResourcePath);
        return null;
      }

      String schemaName = nameElement.getAsString();
      JsonSchema jsonSchema =
          JsonSchema.builder().name(schemaName).rootElement(rootElement).build();

      ResponseFormat responseFormat =
          ResponseFormat.builder().type(ResponseFormatType.JSON).jsonSchema(jsonSchema).build();

      log.debug("Loaded structured output schema '{}' from {}", schemaName, schemaResourcePath);
      return responseFormat;
    } catch (Exception e) {
      log.warn(
          "Failed to load structured output schema from {}. Falling back to free-form responses",
          schemaResourcePath,
          e);
      return null;
    }
  }

  private JsonSchemaElement buildJsonSchemaElement(JsonObject schemaObject) {
    String type = getString(schemaObject, "type");
    if (type == null) {
      if (schemaObject.has("enum")) {
        return buildEnumSchema(schemaObject);
      }
      throw new IllegalArgumentException("Schema definition missing type: " + schemaObject);
    }

    return switch (type) {
      case "object" -> buildObjectSchema(schemaObject);
      case "array" -> buildArraySchema(schemaObject);
      case "string" ->
          schemaObject.has("enum")
              ? buildEnumSchema(schemaObject)
              : buildStringSchema(schemaObject);
      case "integer" -> buildIntegerSchema(schemaObject);
      case "number" -> buildNumberSchema(schemaObject);
      case "boolean" -> buildBooleanSchema(schemaObject);
      default -> throw new IllegalArgumentException("Unsupported schema type: " + type);
    };
  }

  private JsonSchemaElement buildObjectSchema(JsonObject schemaObject) {
    JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
    setDescription(schemaObject, builder::description);

    JsonObject properties = getObject(schemaObject, "properties");
    if (properties != null) {
      for (var entry : properties.entrySet()) {
        JsonObject propertySchema = asObject(entry.getValue());
        if (propertySchema != null) {
          builder.addProperty(entry.getKey(), buildJsonSchemaElement(propertySchema));
        }
      }
    }

    JsonArray required = getArray(schemaObject, "required");
    if (required != null) {
      builder.required(toStringList(required));
    }

    JsonElement additionalProperties = schemaObject.get("additionalProperties");
    if (additionalProperties != null
        && additionalProperties.isJsonPrimitive()
        && additionalProperties.getAsJsonPrimitive().isBoolean()) {
      builder.additionalProperties(additionalProperties.getAsBoolean());
    }

    return builder.build();
  }

  private JsonSchemaElement buildArraySchema(JsonObject schemaObject) {
    JsonArraySchema.Builder builder = JsonArraySchema.builder();
    setDescription(schemaObject, builder::description);

    JsonObject items = asObject(schemaObject.get("items"));
    if (items != null) {
      builder.items(buildJsonSchemaElement(items));
    }

    return builder.build();
  }

  private JsonSchemaElement buildStringSchema(JsonObject schemaObject) {
    JsonStringSchema.Builder builder = JsonStringSchema.builder();
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private JsonSchemaElement buildIntegerSchema(JsonObject schemaObject) {
    JsonIntegerSchema.Builder builder = JsonIntegerSchema.builder();
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private JsonSchemaElement buildNumberSchema(JsonObject schemaObject) {
    JsonNumberSchema.Builder builder = JsonNumberSchema.builder();
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private JsonSchemaElement buildBooleanSchema(JsonObject schemaObject) {
    JsonBooleanSchema.Builder builder = JsonBooleanSchema.builder();
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private JsonSchemaElement buildEnumSchema(JsonObject schemaObject) {
    JsonArray enumValues = getArray(schemaObject, "enum");
    if (enumValues == null || enumValues.isEmpty()) {
      throw new IllegalArgumentException(
          "Enum schema requires non-empty 'enum' array: " + schemaObject);
    }
    JsonEnumSchema.Builder builder = JsonEnumSchema.builder().enumValues(toStringList(enumValues));
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private static JsonObject getObject(JsonObject source, String member) {
    JsonElement element = source.get(member);
    return asObject(element);
  }

  private static JsonObject asObject(JsonElement element) {
    if (element != null && element.isJsonObject()) {
      return element.getAsJsonObject();
    }
    return null;
  }

  private static JsonArray getArray(JsonObject source, String member) {
    JsonElement element = source.get(member);
    if (element != null && element.isJsonArray()) {
      return element.getAsJsonArray();
    }
    return null;
  }

  private static String getString(JsonObject source, String member) {
    JsonElement element = source.get(member);
    if (element != null && element.isJsonPrimitive()) {
      JsonPrimitive primitive = element.getAsJsonPrimitive();
      if (primitive.isString()) {
        return primitive.getAsString();
      }
    }
    return null;
  }

  private static List<String> toStringList(JsonArray array) {
    List<String> values = new ArrayList<>(array.size());
    for (JsonElement element : array) {
      if (element != null && element.isJsonPrimitive()) {
        values.add(element.getAsJsonPrimitive().getAsString());
      }
    }
    return values;
  }

  private static void setDescription(JsonObject source, Consumer<String> consumer) {
    String description = getString(source, "description");
    if (description != null && !description.isBlank()) {
      consumer.accept(description);
    }
  }
}
