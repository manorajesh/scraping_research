package com.projectleo.scraper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

public class GenericScraper implements Scraper {
    private static final Logger logger = LogManager.getLogger(GenericScraper.class);
    private final HttpClient client;
    private final OpenAIClient openAIClient;

    public GenericScraper() {
        this.client = HttpClient.newHttpClient();
        this.openAIClient = new OpenAIClient();
    }

    @Override
    public CompletableFuture<List<String>> fetchJobLinks(String url) {
        logger.info("Fetching job links from URL: {}", url);
        return client.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenCompose(responseBody -> extractJobLinks(responseBody, url));
    }

    private CompletableFuture<List<String>> extractJobLinks(String responseBody, String url) {
        List<String> links = Jsoup.parse(responseBody)
                .select("a")
                .stream()
                .map(element -> element.attr("href").trim())
                .collect(Collectors.toList());

        String hrefs = String.join(",", links);
        logger.info("Extracted hrefs: {}", hrefs);

        OpenAIResponse openAIResponse = openAIClient.getOpenAIResponse(
                "Of these links, which ones most likely forward to the details of the particular job. Respond only in a valid JSON array of strings:",
                hrefs).join();

        List<String> jobLinks = parseOpenAIResponse(
                openAIResponse.choices.get(0).message.content.replaceAll("```json", "").replaceAll("```", ""));
        logger.info("Parsed job links: {}", jobLinks);

        // Convert relative links to absolute links
        jobLinks = jobLinks.stream()
                .map(link -> URI.create(url).resolve(link).toString())
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(jobLinks);
    }

    private List<String> parseOpenAIResponse(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response);
            List<String> jobLinks = jsonNode.isArray()
                    ? mapper.convertValue(jsonNode, List.class)
                    : List.of();
            logger.info("Parsed job links: {}", jobLinks);
            return jobLinks;
        } catch (IOException e) {
            logger.error("Error parsing OpenAI response: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<String> fetchJobDetails(String jobLink) {
        logger.info("Fetching job details from link: {}", jobLink);
        return client
                .sendAsync(HttpRequest.newBuilder(URI.create(jobLink)).build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    @Override
    public CompletableFuture<JobResult> parseJobDetails(String jobDetails) {
        logger.info("Parsing job details");
        String body = Jsoup.parse(jobDetails).body().text();
        logger.info("Parsed job details: {}", body);

        return openAIClient.getOpenAIResponse(
                "Can you give me the company (string), jobTitle (string), location (string), industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:",
                body)
                .thenApply(response -> {
                    try {
                        JobResult jobResult = JobResult.fromCompleteJson(response.choices.get(0).message.content
                                .replaceAll("```json", "").replaceAll("```", ""), 0.0);
                        logger.info("Parsed job result: {}", jobResult.asString());
                        return jobResult;
                    } catch (IOException e) {
                        logger.error("Error parsing job details: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
    }
}
