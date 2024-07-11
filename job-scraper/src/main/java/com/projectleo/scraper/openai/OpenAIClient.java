package com.projectleo.scraper.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectleo.scraper.util.PropertiesUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OpenAIClient {
  private static final Logger logger = LogManager.getLogger(OpenAIClient.class);
  private static final String API_KEY = PropertiesUtil.getProperty("openai.api.key");
  private static final double PROMPT_TOKEN_PRICE = 0.0000005;
  private static final double COMPLETION_TOKEN_PRICE = 0.0000015;

  private static double totalCost = 0.0;

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OpenAIClient() {
    if (API_KEY == null || API_KEY.isEmpty()) {
      logger.error(
          "OpenAI API key is not set. Please set the OPENAI_API_KEY environment variable.");
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
      return sendRequestWithRetry(request, 0);
    } catch (Exception e) {
      logger.error("Error creating request to OpenAI: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<OpenAIResponse> sendRequestWithRetry(
      HttpRequest request, int retryCount) {
    return httpClient
        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenCompose(
            response -> {
              if (response.statusCode() != 200) {
                return handleErrorResponse(response, request, retryCount);
              }
              OpenAIResponse openAIResponse = deserializeResponse(response.body());
              double requestCost = calculateRequestCost(openAIResponse);
              updateTotalCost(requestCost);
              logger.info("Request cost: ${}, Total cost: ${}", requestCost, totalCost);
              return CompletableFuture.completedFuture(openAIResponse);
            });
  }

  private String createRequestBody(String prompt, String description) throws Exception {
    String combinedContent = prompt + description;
    ChatRequest chatRequest =
        new ChatRequest("gpt-3.5-turbo", new Message("user", combinedContent));
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

  private CompletableFuture<OpenAIResponse> handleErrorResponse(
      HttpResponse<String> response, HttpRequest request, int retryCount) {
    try {
      OpenAIError error = objectMapper.readValue(response.body(), OpenAIError.class);
      logger.debug("OpenAI API error: {}", error.toString());

      if ("rate_limit_exceeded".equals(error.getError().getCode()) && retryCount < 3) {
        // Extract the wait time using regex
        String message = error.getError().getMessage();
        String regex = "(?<=Please try again in )\\d+(?=ms\\.)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        long waitTime = 1000; // default wait time if no match found
        if (matcher.find()) {
          waitTime = Long.parseLong(matcher.group());
        }
        logger.info("Rate limit exceeded. Retrying in {} ms...", waitTime);
        Thread.sleep(waitTime);
        return sendRequestWithRetry(request, retryCount + 1);
      } else if (retryCount == 3) {
        throw new RuntimeException("Retry limit reached.");
      } else if (retryCount < 3) {
        logger.error("OpenAI API error: {}", error.getError().getMessage());
        logger.info("Retrying request...");
        return sendRequestWithRetry(request, retryCount + 1);
      } else {
        throw new RuntimeException("OpenAI API error: " + error);
      }
    } catch (Exception e) {
      logger.error("Error handling OpenAI error response: {}", e.getMessage(), e, response.body());
      throw new RuntimeException("Error handling OpenAI error response", e);
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
      this.messages = new Message[] {message};
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
