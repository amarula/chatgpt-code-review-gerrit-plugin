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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import java.lang.reflect.Field;
import org.junit.Test;

public class LangChainClientTest {

  @Test
  public void shouldLoadStructuredResponseFormatFromSchemaResource() throws Exception {
    LangChainClient client = new LangChainClient(null, null, null, null);

    Field field = LangChainClient.class.getDeclaredField("structuredResponseFormat");
    field.setAccessible(true);
    ResponseFormat responseFormat = (ResponseFormat) field.get(client);

    assertNotNull("Structured response format should be loaded", responseFormat);
    assertEquals(ResponseFormatType.JSON, responseFormat.type());

    JsonSchema jsonSchema = responseFormat.jsonSchema();
    assertNotNull(jsonSchema);
    assertEquals("format_replies", jsonSchema.name());
    assertTrue(jsonSchema.rootElement() instanceof JsonObjectSchema);

    JsonObjectSchema root = (JsonObjectSchema) jsonSchema.rootElement();
    assertTrue(root.properties().containsKey("replies"));
    assertTrue(root.properties().containsKey("changeId"));

    JsonArraySchema repliesSchema = (JsonArraySchema) root.properties().get("replies");
    assertNotNull(repliesSchema.items());
    assertTrue(repliesSchema.items() instanceof JsonObjectSchema);
    JsonObjectSchema replyItemSchema = (JsonObjectSchema) repliesSchema.items();
    assertTrue(replyItemSchema.properties().containsKey("reply"));
  }
}
