package com.platform.page.page.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.page.page.dto.GeneratePageRequest;
import com.platform.page.page.dto.GeneratedPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageGenerationService {

    private static final String SYSTEM_PROMPT = """
            You are an expert UI designer for a low-code platform. Your task is to generate a page definition
            in JSON format based on the user's natural language description.

            The JSON must conform exactly to this schema:
            {
              "version": "1.0",
              "title": "<page title>",
              "description": "<optional short description>",
              "layout": {
                "type": "sections",
                "sections": [
                  {
                    "id": "<unique snake_case id>",
                    "title": "<optional section title>",
                    "columns": <1|2|3|4>,
                    "widgets": [
                      {
                        "id": "<unique snake_case id>",
                        "type": "<kpi|table|form|text|chart>",
                        "title": "<optional widget title>",
                        "colSpan": <1..4, optional>,
                        "config": { ... }
                      }
                    ]
                  }
                ]
              }
            }

            Widget config shapes per type:
            - kpi:   { "label": "string", "dataSource": { "url": "/api/v1/metrics/...", "valueField": "string", "unitField": "string" }, "icon": "users|tasks|chart|alert|check", "trend": true|false }
            - table: { "dataSource": { "url": "/api/v1/...", "pageSize": 10 }, "columns": [{ "field": "string", "header": "string", "type": "text|date|badge" }], "searchable": true }
            - form:  { "formKey": "string", "submitUrl": "/api/v1/...", "successMessage": "string" }
            - text:  { "content": "string", "variant": "info|warning|success|default" }
            - chart: { "chartType": "bar|line|pie", "dataSource": { "url": "/api/v1/...", "labelField": "string", "valueField": "string" } }

            Rules:
            - Use realistic but placeholder API URLs under /api/v1/
            - Choose 2-6 sections and appropriate widgets for the described use case
            - Prefer 2 or 3 columns for dashboards; 1 column for forms and detail pages
            - Make IDs descriptive, unique, and in snake_case
            - Return ONLY a JSON object with these top-level keys: "suggestedPageKey" (kebab-case slug), "suggestedName" (human readable name), "schema" (the PageSchema JSON as a string)
            - Do NOT wrap the response in markdown code fences
            """;

    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public GeneratedPageResponse generate(GeneratePageRequest req) {
        log.info("Generating page for prompt: {}", req.prompt());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of("claude-opus-4-8"))
                .maxTokens(8000L)
                .system(SYSTEM_PROMPT)
                .addUserMessage("Generate a page definition for: " + req.prompt())
                .build();

        Message message = anthropicClient.messages().create(params);

        String raw = message.content().stream()
                .flatMap(block -> block.text().stream())
                .findFirst()
                .map(t -> t.text())
                .orElseThrow(() -> new IllegalStateException("No text in Claude response"));

        return parseResponse(raw);
    }

    private GeneratedPageResponse parseResponse(String raw) {
        String json = raw.strip();
        // Strip markdown fences if the model added them despite instructions
        if (json.startsWith("```")) {
            int start = json.indexOf('\n') + 1;
            int end = json.lastIndexOf("```");
            json = end > start ? json.substring(start, end).strip() : json;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            String pageKey = node.path("suggestedPageKey").asText();
            String name = node.path("suggestedName").asText();
            JsonNode schemaNode = node.path("schema");

            String schemaStr;
            if (schemaNode.isTextual()) {
                // Claude returned schema as a JSON string — validate it's parseable
                objectMapper.readTree(schemaNode.asText());
                schemaStr = schemaNode.asText();
            } else if (schemaNode.isObject()) {
                // Claude returned schema as a nested object — serialize it
                schemaStr = objectMapper.writeValueAsString(schemaNode);
            } else {
                throw new IllegalStateException("Unexpected 'schema' field type in Claude response");
            }

            return new GeneratedPageResponse(pageKey, name, schemaStr);
        } catch (Exception e) {
            log.error("Failed to parse Claude response as GeneratedPageResponse: {}", json, e);
            throw new IllegalStateException("AI returned an unexpected format. Please try again.", e);
        }
    }
}
