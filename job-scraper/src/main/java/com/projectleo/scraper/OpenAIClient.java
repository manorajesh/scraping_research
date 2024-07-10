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
        private static final double PROMPT_TOKEN_PRICE = 0.0000005;
        private static final double COMPLETION_TOKEN_PRICE = 0.0000015;

        private static double totalCost = 0.0;

        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        public OpenAIClient() {
                if (API_KEY == null || API_KEY.isEmpty()) {
                        logger.error("OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
                        throw new IllegalStateException(
                                        "OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
                }

                this.httpClient = HttpClient.newHttpClient();
                this.objectMapper = new ObjectMapper();
        }

        public CompletableFuture<OpenAIResponse> getOpenAIResponse(String prompt, String description) {
                try {
                        String requestBody = createRequestBody(prompt, description);
                        HttpRequest request = createHttpRequest(requestBody);

                        logger.info("Sending request to OpenAI");
                        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                        .thenApply(response -> {
                                                if (response.statusCode() != 200) {
                                                        handleErrorResponse(response);
                                                }
                                                OpenAIResponse openAIResponse = deserializeResponse(response.body());
                                                double requestCost = calculateRequestCost(openAIResponse);
                                                updateTotalCost(requestCost);
                                                logger.info("Request cost: ${}, Total cost: ${}", requestCost,
                                                                totalCost);
                                                return openAIResponse;
                                        });
                } catch (Exception e) {
                        logger.error("Error creating request to OpenAI: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                }
        }

        private String createRequestBody(String prompt, String description) throws Exception {
                String combinedContent = prompt + description;
                ChatRequest chatRequest = new ChatRequest("gpt-3.5-turbo", new Message("user", combinedContent));
                return objectMapper.writeValueAsString(chatRequest);
        }

        private HttpRequest createHttpRequest(String requestBody) {
                return HttpRequest.newBuilder()
                                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + API_KEY)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();
        }

        private OpenAIResponse deserializeResponse(String responseBody) {
                try {
                        return objectMapper.readValue(responseBody, OpenAIResponse.class);
                } catch (Exception e) {
                        logger.error("Error deserializing OpenAI response: {}", e.getMessage(), e);
                        throw new RuntimeException(e);
                }
        }

        private void handleErrorResponse(HttpResponse<String> response) {
                try {
                        OpenAIError error = objectMapper.readValue(response.body(), OpenAIError.class);
                        logger.error("OpenAI API error: {}", error);
                        throw new RuntimeException("OpenAI API error: " + error);
                } catch (Exception e) {
                        logger.error("Error deserializing OpenAI error response: {}", response.body(), e);
                        throw new RuntimeException("Error deserializing OpenAI error response", e);
                }
        }

        private double calculateRequestCost(OpenAIResponse response) {
                int promptTokens = response.getPromptTokens();
                int completionTokens = response.getCompletionTokens();

                return promptTokens * PROMPT_TOKEN_PRICE + completionTokens * COMPLETION_TOKEN_PRICE;
        }

        private static synchronized void updateTotalCost(double cost) {
                totalCost += cost;
        }

        public static double getTotalCost() {
                return totalCost;
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
