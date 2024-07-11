package com.projectleo.scraper;

import com.projectleo.scraper.database.Database;
import com.projectleo.scraper.openai.OpenAIClient;
import com.projectleo.scraper.scrapers.GenericScraper;
import com.projectleo.scraper.scrapers.JobResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
  private static final Logger logger = LogManager.getLogger(Main.class);
  private static final List<String> COMPANY_URLS =
      List.of(
          "https://boards.greenhouse.io/radiant",
          "https://jobs.lever.co/make-rain",
          "https://us241.dayforcehcm.com/CandidatePortal/en-US/nantmedia",
          "https://boards.greenhouse.io/goguardian",
          "https://jobs.lever.co/ablspacesystems",
          "https://jobs.mattel.com/en/search-jobs/El%20Segundo%2C%20CA/",
          "https://www.spacex.com/careers/jobs?location=hawthorne%252C%2520ca",
          "https://www.disneycareers.com/en/search-jobs/Los%20Angeles%2C%20CA",
          "https://jobs.boeing.com/search-jobs/El%20Segundo%2C%20CA/",
          "https://jobs.netflix.com/search");

  public static void main(String[] args) {
    try (ExecutorService executor = Executors.newFixedThreadPool(10);
        Database database = new Database()) {
      Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));

      GenericScraper scraper = new GenericScraper(database);

      List<CompletableFuture<Void>> companyFutures =
          COMPANY_URLS.stream()
              .map(companyUrl -> processCompany(scraper, companyUrl, database))
              .collect(Collectors.toList());

      CompletableFuture.allOf(companyFutures.toArray(new CompletableFuture[0])).join();
    }

    logger.info("Finished processing all company URLs");
    double totalCost = OpenAIClient.getTotalCost();
    logger.info("Total cost of running the program: ${}", totalCost);
  }

  private static CompletableFuture<Void> processCompany(
      GenericScraper scraper, String companyUrl, Database database) {
    return scraper
        .fetchJobLinks(companyUrl)
        .thenCompose(jobLinks -> fetchAndParseJobDetails(scraper, jobLinks))
        .thenAccept(jobResults -> writeJobResultsToDatabase(jobResults, database))
        .exceptionally(
            ex -> {
              logger.error("Error processing URL {}: {}", companyUrl, ex.getMessage());
              return null;
            });
  }

  private static CompletableFuture<List<JobResult>> fetchAndParseJobDetails(
      GenericScraper scraper, List<String> jobLinks) {
    List<CompletableFuture<JobResult>> jobResultFutures =
        jobLinks.stream()
            .map(
                jobLink ->
                    scraper
                        .fetchJobDetails(jobLink)
                        .thenCompose(jobDetails -> scraper.parseJobDetails(jobDetails, jobLink)))
            .collect(Collectors.toList());

    return CompletableFuture.allOf(jobResultFutures.toArray(new CompletableFuture[0]))
        .thenApply(
            v ->
                jobResultFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
  }

  private static void writeJobResultsToDatabase(List<JobResult> jobResults, Database database) {
    synchronized (Main.class) {
      database.writeToDatabase(jobResults);
    }
  }
}
