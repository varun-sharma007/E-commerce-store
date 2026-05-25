package com.ecom.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate;

    public GeminiService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    @SuppressWarnings("unchecked")
    public String generate(String systemPrompt, String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                   + model + ":generateContent?key=" + apiKey;

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt)));

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage)));

        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", 300,
                "temperature",     0.7,
                "topP",            0.9);

        Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction,
                "contents",          List.of(userContent),
                "generationConfig",  generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) return null;

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            Object text = parts.get(0).get("text");
            return text != null ? text.toString().trim() : null;

        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            System.err.println("[Gemini] Quota exhausted.");
            return null;
        } catch (Exception e) {
            System.err.println("[Gemini] Error: " + e.getMessage());
            return null;
        }
    }
}
