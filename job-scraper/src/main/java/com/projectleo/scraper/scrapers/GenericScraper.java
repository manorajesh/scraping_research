package com.projectleo.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectleo.scraper.database.Database;
import com.projectleo.scraper.openai.OpenAIClient;
import com.projectleo.scraper.openai.OpenAIResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

public class GenericScraper implements Scraper {
  private static final Logger logger = LogManager.getLogger(GenericScraper.class);
  private final HttpClient client;
  private final OpenAIClient openAIClient;
  private final Database database;
  private static final int MAX_CONCURRENT_REQUESTS = 200;
  private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

  // Maximum number of job links to fetch per company
  private final int maxCompanyJobLinks;

  public GenericScraper(Database database, int maxCompanyJobLinks) {
    this.client = HttpClient.newHttpClient();
    this.openAIClient = new OpenAIClient();
    this.database = database;
    this.maxCompanyJobLinks = maxCompanyJobLinks;
  }

  @Override
  public CompletableFuture<List<String>> fetchJobLinks(String url) {
    logger.info("Fetching job links from URL: {}", url);
    return CompletableFuture.supplyAsync(
            () -> {
              try {
                logger.debug("Available permits before acquire: {}", semaphore.availablePermits());
                semaphore.acquire();
                logger.debug(
                    "Semaphore acquired for fetching job links, available permits: {}",
                    semaphore.availablePermits());
                return null;
              } catch (InterruptedException e) {
                logger.error("Semaphore acquisition interrupted", e);
                throw new RuntimeException(e);
              }
            })
        .thenCompose(
            ignored ->
                client.sendAsync(
                    HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofString()))
        .thenApply(HttpResponse::body)
        .thenCompose(responseBody -> extractJobLinks(responseBody, url))
        .handle(
            (result, error) -> {
              semaphore.release();
              logger.debug(
                  "Semaphore released after fetching job links, available permits: {}",
                  semaphore.availablePermits());
              if (error != null) {
                logger.error("Error during job link fetch", error);
                throw new RuntimeException(error);
              }
              return result;
            });
  }

  private CompletableFuture<List<String>> extractJobLinks(String responseBody, String url) {
    List<String> links =
        Jsoup.parse(responseBody).select("a").stream()
            .map(element -> element.attr("href").trim())
            .collect(Collectors.toList());

    String hrefs = String.join(",", links);
    logger.debug("Extracted hrefs: {}", hrefs);
    logger.info("Extracted {} job links from URL: {}", links.size(), url);

    return openAIClient
        .getOpenAIResponse(
            "What is the valid, escaped, concise regex pattern that would match all the following"
                + " hrefs that forward to their respective job details site. Respond in JSON with"
                + " the key being \"regex_pattern\"",
            hrefs)
        .thenApply(response -> parseAndResolveJobLinks(response, links, url));
  }

  private List<String> parseAndResolveJobLinks(
      OpenAIResponse response, List<String> links, String baseUrl) {
    try {
      String regexPattern = parseOpenAIResponse(response.choices.get(0).message.content);
      logger.debug("Parsed regex pattern: {}", regexPattern);

      List<String> jobLinks =
          links.stream().filter(link -> link.matches(regexPattern)).collect(Collectors.toList());
      logger.debug("Filtered job links: {}", jobLinks);
      logger.info("Filtered and resolved {} job links successfully", jobLinks.size());

      // Convert relative links to absolute links
      List<String> resolvedJobLinks =
          jobLinks.stream()
              .map(link -> URI.create(baseUrl).resolve(link).toString())
              .collect(Collectors.toList());

      // Filter out job links that already exist in the database
      return filterUniqueJobLinks(resolvedJobLinks);
    } catch (RuntimeException e) {
      logger.error("Error resolving job links: {}", e.getMessage());
      throw e;
    }
  }

  private String parseOpenAIResponse(String response) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(response);
      return jsonNode.get("regex_pattern").asText();
    } catch (IOException e) {
      logger.error("Error parsing OpenAI response: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private List<String> filterUniqueJobLinks(List<String> jobLinks) {
    List<String> uniqueJobLinks = new ArrayList<>();
    for (String jobLink : jobLinks) {
      byte[] jobLinkHash = generateJobLinkHash(jobLink);
      if (!database.jobLinkExists(jobLinkHash)) {
        uniqueJobLinks.add(jobLink);
      } else {
        logger.info("Job link already exists in database: {}. Skipping...", jobLink);
      }
    }

    // Limit the number of job links to fetch per company
    if (uniqueJobLinks.size() > maxCompanyJobLinks) {
      logger.info("Limiting job links to {} from {}", maxCompanyJobLinks, uniqueJobLinks.size());
      return uniqueJobLinks.subList(0, maxCompanyJobLinks);
    }
    return uniqueJobLinks;
  }

  // TODO: Coalesce this method with the one in JobResult
  private byte[] generateJobLinkHash(String jobLink) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(jobLink.getBytes());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletableFuture<String> fetchJobDetails(String jobLink) {
    logger.info("Fetching job details from link: {}", jobLink);
    return CompletableFuture.supplyAsync(
            () -> {
              try {
                logger.debug("Available permits before acquire: {}", semaphore.availablePermits());
                semaphore.acquire();
                logger.debug(
                    "Semaphore acquired for fetching job details, available permits: {}",
                    semaphore.availablePermits());
                return null;
              } catch (InterruptedException e) {
                logger.error("Semaphore acquisition interrupted", e);
                throw new RuntimeException(e);
              }
            })
        .thenCompose(
            ignored ->
                client.sendAsync(
                    HttpRequest.newBuilder(URI.create(jobLink)).build(),
                    HttpResponse.BodyHandlers.ofString()))
        .thenApply(HttpResponse::body)
        .handle(
            (result, error) -> {
              semaphore.release();
              logger.debug(
                  "Semaphore released after fetching job details, available permits: {}",
                  semaphore.availablePermits());
              if (error != null) {
                logger.error("Error during job detail fetch", error);
                throw new RuntimeException(error);
              }
              return result;
            });
  }

  @Override
  public CompletableFuture<JobResult> parseJobDetails(String jobDetails, String jobLink) {
    logger.info("Parsing job details");
    String body = Jsoup.parse(jobDetails).body().text();
    logger.trace("HTML body at url {}: {}", jobLink, body);

    return openAIClient
        .getOpenAIResponse(
            "Can you give me the company (string), jobTitle (string), location (string), industry"
                + " (string), responsibilities (array of strings), qualifications (array of"
                + " strings), and skills (array of strings) of this job listing in valid JSON. Find"
                + " as many responsibilities and qualifications as it says. Find the high-level but"
                + " detailed skills that are relevant to this job:",
            body)
        .thenApply(
            response -> {
              try {
                String jsonResponse =
                    response
                        .choices
                        .get(0)
                        .message
                        .content
                        .replaceAll("```json", "")
                        .replaceAll("```", "");
                JobResult jobResult = JobResult.fromCompleteJson(jsonResponse);
                jobResult.setJobLinkHash(jobLink);
                // TODO: This doesn't write to database. CURRENT_TIMESTAMP is default value
                jobResult.setTimestamp(LocalDateTime.now());
                logger.debug("Parsed job result: {}", jobResult.asString());
                logger.info(
                    "Parsed job details successfully: {} - {}, {} skills",
                    jobResult.getCompany(),
                    jobResult.getJobTitle(),
                    jobResult.getSkills().size());
                return jobResult;
              } catch (IOException e) {
                logger.error("Error parsing job details: {}", e.getMessage());
                throw new RuntimeException(e);
              }
            });
  }
}
