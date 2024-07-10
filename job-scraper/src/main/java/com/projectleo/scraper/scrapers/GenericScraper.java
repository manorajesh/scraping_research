package com.projectleo.scraper.scrapers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectleo.scraper.openai.OpenAIClient;
import com.projectleo.scraper.openai.OpenAIResponse;

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
        logger.debug("Extracted hrefs: {}", hrefs);
        logger.info("Extracted {} job links from URL: {}", links.size(), url);

        return openAIClient.getOpenAIResponse(
                "Of these links, which ones most likely forward to the details of the particular job. Respond only in a valid JSON array of strings:",
                hrefs)
                .thenApply(response -> parseAndResolveJobLinks(response, url));
    }

    private List<String> parseAndResolveJobLinks(OpenAIResponse response, String baseUrl) {
        try {
            List<String> jobLinks = parseOpenAIResponse(
                    response.choices.get(0).message.content.replaceAll("```json", "").replaceAll("```", ""));
            logger.debug("Parsed job links: {}", jobLinks);
            logger.info("Parsed and resolved {} job links successfully", jobLinks.size());

            // Convert relative links to absolute links
            return jobLinks.stream()
                    .map(link -> URI.create(baseUrl).resolve(link).toString())
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            logger.error("Error resolving job links: {}", e.getMessage());
            throw e;
        }
    }

    private List<String> parseOpenAIResponse(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response);
            List<String> jobLinks = jsonNode.isArray()
                    ? mapper.convertValue(jsonNode, new TypeReference<List<String>>() {
                    })
                    : List.of();
            logger.debug("Parsed job links: {}", jobLinks);
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
        logger.debug("HTML body: {}", body);

        return openAIClient.getOpenAIResponse(
                "Can you give me the company (string), jobTitle (string), location (string), industry (string), responsibilities (array of strings), and qualifications (array of strings) of this job listing in valid JSON. Find as many responsibilities and qualifications as it says:",
                body)
                .thenApply(response -> {
                    try {
                        String jsonResponse = response.choices.get(0).message.content
                                .replaceAll("```json", "").replaceAll("```", "");
                        JobResult jobResult = JobResult.fromCompleteJson(jsonResponse, 0.0);
                        logger.debug("Parsed job result: {}", jobResult.asString());
                        logger.info("Parsed job details successfully: {} - {}", jobResult.getCompany(),
                                jobResult.getJobTitle());
                        return jobResult;
                    } catch (IOException e) {
                        logger.error("Error parsing job details: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
    }
}
