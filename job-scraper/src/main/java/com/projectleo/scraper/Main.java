package com.projectleo.scraper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.projectleo.scraper.database.CsvUtil;
import com.projectleo.scraper.openai.OpenAIClient;
import com.projectleo.scraper.scrapers.GenericScraper;
import com.projectleo.scraper.scrapers.JobResult;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final String FILENAME = "jobs.csv";
    private static final List<String> COMPANY_URLS = List.of(
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
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));

            GenericScraper scraper = new GenericScraper();

            List<CompletableFuture<Void>> companyFutures = COMPANY_URLS.stream()
                    .map(companyUrl -> processCompany(scraper, companyUrl))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(companyFutures.toArray(new CompletableFuture[0])).join();
        }

        logger.info("Finished processing all company URLs");
        double totalCost = OpenAIClient.getTotalCost();
        logger.info("Total cost of running the program: ${}", totalCost);
    }

    private static CompletableFuture<Void> processCompany(GenericScraper scraper, String companyUrl) {
        return scraper.fetchJobLinks(companyUrl)
                .thenCompose(jobLinks -> fetchAndParseJobDetails(scraper, jobLinks))
                .thenAccept(jobResults -> writeJobResultsToCsv(jobResults, companyUrl))
                .exceptionally(ex -> {
                    logger.error("Error processing URL {}: {}", companyUrl, ex.getMessage());
                    return null;
                });
    }

    private static CompletableFuture<List<JobResult>> fetchAndParseJobDetails(GenericScraper scraper,
            List<String> jobLinks) {
        List<CompletableFuture<JobResult>> jobResultFutures = jobLinks.stream()
                .map(scraper::fetchJobDetails)
                .map(jobDetailsFuture -> jobDetailsFuture.thenCompose(scraper::parseJobDetails))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(jobResultFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> jobResultFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    private static void writeJobResultsToCsv(List<JobResult> jobResults, String companyUrl) {
        synchronized (Main.class) {
            try {
                CsvUtil.writeCsv(FILENAME, jobResults);
            } catch (IOException e) {
                logger.error("Error writing CSV file for URL {}: {}", companyUrl, e.getMessage());
            }
        }
    }
}
