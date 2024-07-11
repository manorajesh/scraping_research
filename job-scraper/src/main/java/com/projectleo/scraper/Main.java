package com.projectleo.scraper;

import com.projectleo.scraper.database.Database;
import com.projectleo.scraper.openai.OpenAIClient;
import com.projectleo.scraper.scrapers.GenericScraper;
import com.projectleo.scraper.scrapers.JobResult;
import com.projectleo.scraper.util.FileUtil;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
  private static final Logger logger = LogManager.getLogger(Main.class);
  private static final String COMPANY_URLS_FILE = "company_urls.txt";

  public static void main(String[] args) {
    List<String> companyUrls;

    try {
      companyUrls = FileUtil.readLines(COMPANY_URLS_FILE);
    } catch (IOException e) {
      logger.error("Failed to read company URLs from file: {}", e.getMessage());
      return;
    }

    try (ExecutorService executor = Executors.newFixedThreadPool(20);
        Database database = new Database()) {
      Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));

      GenericScraper scraper = new GenericScraper(database, 100);

      List<CompletableFuture<Void>> companyFutures =
          companyUrls.stream()
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
