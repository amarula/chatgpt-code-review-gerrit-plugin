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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.ai.AiClientBase;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritChange;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.api.gerrit.GerritClient;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiHistory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.client.prompt.AiPromptFactory;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.api.ai.AiResponseContent;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.GerritClientData;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.messages.LangChainChatMessages;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.model.LangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.LangChainProviderFactory;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.errors.exceptions.AiConnectionFailException;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.api.ai.IAiClient;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.common.client.code.context.ICodeContextPolicy;
import com.googlesource.gerrit.plugins.reviewai.interfaces.aibackend.langchain.provider.ILangChainProvider;
import com.googlesource.gerrit.plugins.reviewai.localization.Localizer;
import com.googlesource.gerrit.plugins.reviewai.settings.Settings.LangChainProviders;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
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
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.isJsonObjectAsString;
import static com.googlesource.gerrit.plugins.reviewai.utils.JsonTextUtils.unwrapJsonCode;

@Slf4j
@Singleton
public class LangChainClient extends AiClientBase implements IAiClient {

  private static final String FORMAT_REPLIES_SCHEMA_RESOURCE = "config/formatRepliesTool.json";

  private final ICodeContextPolicy codeContextPolicy;
  private final LangChainTokenEstimatorProvider tokenEstimatorProvider;
  private final GerritClient gerritClient;
  private final Localizer localizer;
  private final ResponseFormat structuredResponseFormat;

  private String requestBody;

  @Inject
  public LangChainClient(
      Configuration config,
      ICodeContextPolicy codeContextPolicy,
      GerritClient gerritClient,
      Localizer localizer) {
    super(config);
    this.codeContextPolicy = codeContextPolicy;
    this.tokenEstimatorProvider = new LangChainTokenEstimatorProvider(config);
    this.gerritClient = gerritClient;
    this.localizer = localizer;
    this.structuredResponseFormat = loadStructuredResponseFormat();
    log.debug("Initialized LangChainClient");
  }

  @Override
  public AiResponseContent ask(
      ChangeSetData changeSetData, GerritChange change, String patchSet) throws Exception {
    try {
      var prompt = AiPromptFactory.getAiPrompt(config, changeSetData, change, codeContextPolicy);
      String systemInstructions = prompt.getDefaultAiAssistantInstructions();
      String userMessage = prompt.getDefaultAiThreadReviewMessage(patchSet);
      Object memoryId = change.getFullChangeId();

      log.info("LangChain system instructions for {}: {}", memoryId, systemInstructions);
      log.info("LangChain user prompt for {} patchSet {}: {}", memoryId, patchSet, userMessage);

      ChatMemory memory =
          TokenWindowChatMemory.builder()
              .id(memoryId)
              .maxTokens(config.getLcMaxMemoryTokens(), tokenEstimatorProvider.get())
              .build();

      memory.add(LangChainChatMessages.systemMessage(systemInstructions));

      GerritClientData gerritClientData = gerritClient.getClientData(change);
      AiHistory aiHistory = new AiHistory(config, changeSetData, gerritClientData, localizer);
      List<ChatMessage> history =
          LangChainChatMessages.build(aiHistory, gerritClientData, change);
      for (ChatMessage message : history) {
        memory.add(message);
      }

      memory.add(LangChainChatMessages.userMessage(userMessage));
      requestBody = userMessage; // exposed for tests/inspection

      double temperature =
          change.getIsCommentEvent()
              ? Double.parseDouble(config.getAiCommentTemperature())
              : Double.parseDouble(config.getAiReviewTemperature());

      LangChainProviders providerType = config.getLcProvider();
      ILangChainProvider provider = LangChainProviderFactory.get(providerType);
      LangChainProvider providerModel = provider.buildChatModel(config, temperature);
      ChatModel model = providerModel.getModel();

      log.info(
          "LangChain request for {} using provider {} model {} (temperature={}, endpoint={})",
          memoryId,
          providerType,
          config.getAiModel(),
          temperature,
          providerModel.getEndpoint());

      List<ChatMessage> memorySnapshot = memory.messages();
      log.debug(
          "LangChain memory prepared for {} with {} messages: {}",
          memoryId,
          memorySnapshot.size(),
          memorySnapshot);

      ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(memorySnapshot);
      if (structuredResponseFormat != null) {
        requestBuilder.responseFormat(structuredResponseFormat);
      }

      ChatResponse response = model.chat(requestBuilder.build());
      AiMessage ai = response != null ? response.aiMessage() : null;
      String responseText = ai != null ? ai.text() : null;

      if (responseText == null) {
        log.warn("LangChain model returned null response text");
        return null;
      }

      if (isJsonObjectAsString(responseText)) {
        return convertResponseContentFromJson(unwrapJsonCode(responseText));
      }
      return new AiResponseContent(responseText);
    } catch (Exception e) {
      log.warn("Error while processing LangChain request", e);
      throw new AiConnectionFailException(e);
    }
  }

  @Override
  public String getRequestBody() {
    return requestBody;
  }

  private ResponseFormat loadStructuredResponseFormat() {
    try (InputStream inputStream =
            LangChainClient.class
                .getClassLoader()
                .getResourceAsStream(FORMAT_REPLIES_SCHEMA_RESOURCE)) {
      if (inputStream == null) {
        log.warn(
            "Structured output schema resource {} not found; falling back to free-form text",
            FORMAT_REPLIES_SCHEMA_RESOURCE);
        return null;
      }

      JsonObject root =
          JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
              .getAsJsonObject();
      JsonObject function = root.getAsJsonObject("function");
      if (function == null) {
        log.warn(
            "Structured output schema resource {} missing 'function' definition; ignoring",
            FORMAT_REPLIES_SCHEMA_RESOURCE);
        return null;
      }

      JsonElement nameElement = function.get("name");
      JsonElement parametersElement = function.get("parameters");
      if (nameElement == null || !nameElement.isJsonPrimitive()) {
        log.warn(
            "Structured output schema resource {} missing function name; ignoring",
            FORMAT_REPLIES_SCHEMA_RESOURCE);
        return null;
      }
      if (parametersElement == null || parametersElement.isJsonNull()) {
        log.warn(
            "Structured output schema resource {} missing function parameters; ignoring",
            FORMAT_REPLIES_SCHEMA_RESOURCE);
        return null;
      }

      String schemaName = nameElement.getAsString();
      JsonSchemaElement rootElement = null;
      if (parametersElement != null && parametersElement.isJsonObject()) {
        try {
          rootElement = buildJsonSchemaElement(parametersElement.getAsJsonObject());
        } catch (Exception e) {
          log.warn(
              "Failed to convert structured output schema {} into LangChain schema classes",
              FORMAT_REPLIES_SCHEMA_RESOURCE,
              e);
        }
      }

      if (rootElement != null) {
        JsonSchema jsonSchema =
            JsonSchema.builder().name(schemaName).rootElement(rootElement).build();

        ResponseFormat responseFormat =
            ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();

        log.debug(
            "Loaded structured output schema '{}' from {}",
            schemaName,
            FORMAT_REPLIES_SCHEMA_RESOURCE);
        return responseFormat;
      }
      log.warn(
          "Structured output schema {} could not be converted; structured responses disabled",
          FORMAT_REPLIES_SCHEMA_RESOURCE);
      return null;
    } catch (Exception e) {
      log.warn(
          "Failed to load structured output schema from {}. Falling back to free-form responses",
          FORMAT_REPLIES_SCHEMA_RESOURCE,
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
          schemaObject.has("enum") ? buildEnumSchema(schemaObject) : buildStringSchema(schemaObject);
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
        JsonObject propertySchema = asObject(entry.getValue(), "properties." + entry.getKey());
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

    JsonObject items = asObject(schemaObject.get("items"), "items");
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
      throw new IllegalArgumentException("Enum schema requires non-empty 'enum' array: " + schemaObject);
    }
    JsonEnumSchema.Builder builder = JsonEnumSchema.builder().enumValues(toStringList(enumValues));
    setDescription(schemaObject, builder::description);
    return builder.build();
  }

  private static JsonObject getObject(JsonObject source, String member) {
    JsonElement element = source.get(member);
    return asObject(element, member);
  }

  private static JsonObject asObject(JsonElement element, String path) {
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

  private static void setDescription(JsonObject source, java.util.function.Consumer<String> consumer) {
    String description = getString(source, "description");
    if (description != null && !description.isBlank()) {
      consumer.accept(description);
    }
  }
}
