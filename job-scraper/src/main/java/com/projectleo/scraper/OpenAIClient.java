package com.projectleo.scraper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenAIClient {
        private static final Logger logger = LogManager.getLogger(OpenAIClient.class);
        private static final String API_KEY = System.getenv("OPENAI_API_KEY");

        public CompletableFuture<OpenAIResponse> getOpenAIResponse(String prompt, String description) {
                try {
                        // Create a JSON object for the request body
                        ObjectMapper mapper = new ObjectMapper();
                        String combinedContent = prompt + description;
                        String requestBody = mapper.writeValueAsString(
                                        new ChatRequest("gpt-3.5-turbo", new Message("user", combinedContent)));

                        HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "Bearer " + API_KEY)
                                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                        .build();

                        logger.info("Sending request to OpenAI");
                        return HttpClient.newHttpClient()
                                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                        .thenApply(response -> {
                                                if (response.statusCode() != 200) {
                                                        logger.error("Error sending request to OpenAI: {}",
                                                                        response.body());
                                                        throw new RuntimeException("Error sending request to OpenAI");
                                                }
                                                return deserializeResponse(response.body());
                                        });
                } catch (Exception e) {
                        logger.error("Error creating request to OpenAI: {}", e.getMessage());
                        throw new RuntimeException(e);
                }
        }

        private OpenAIResponse deserializeResponse(String responseBody) {
                try {
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(responseBody, OpenAIResponse.class);
                } catch (Exception e) {
                        logger.error("Error deserializing OpenAI response: {}", e.getMessage());
                        throw new RuntimeException(e);
                }
        }

        private static class ChatRequest {
                public String model;
                public Message[] messages;

                public ChatRequest(String model, Message message) {
                        this.model = model;
                        this.messages = new Message[] { message };
                }
        }

        private static class Message {
                public String role;
                public String content;

                public Message(String role, String content) {
                        this.role = role;
                        this.content = content;
                }
        }
}
