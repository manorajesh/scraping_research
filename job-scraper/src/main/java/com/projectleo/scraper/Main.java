package com.projectleo.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final String FILENAME = "jobs.csv";
    private static final String[] COMPANY_URLS = {
            "https://boards.greenhouse.io/radiant",
            "https://jobs.lever.co/make-rain",
            "https://us241.dayforcehcm.com/CandidatePortal/en-US/nantmedia",
            "https://boards.greenhouse.io/goguardian",
            "https://radlink.com/careers/",
            "https://jobs.lever.co/ablspacesystems",
            "https://jobs.mattel.com/en/search-jobs/El%20Segundo%2C%20CA/",
            "https://www.spacex.com/careers/jobs?location=hawthorne%252C%2520ca",
            "https://www.disneycareers.com/en/search-jobs/Los%20Angeles%2C%20CA",
            "https://jobs.boeing.com/search-jobs/El%20Segundo%2C%20CA/",
            "https://jobs.netflix.com/search",
    };

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        GenericScraper scraper = new GenericScraper();
        List<CompletableFuture<List<String>>> linkFutures = new ArrayList<>();

        for (String companyUrl : COMPANY_URLS) {
            linkFutures.add(scraper.fetchJobLinks(companyUrl));
        }

        CompletableFuture.allOf(linkFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> linkFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))
                .thenAccept(jobLinks -> {
                    List<CompletableFuture<JobResult>> jobResultFutures = jobLinks.stream()
                            .map(scraper::fetchJobDetails)
                            .map(jobDetailsFuture -> jobDetailsFuture.thenCompose(scraper::parseJobDetails))
                            .collect(Collectors.toList());

                    CompletableFuture.allOf(jobResultFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v2 -> jobResultFutures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList()))
                            .thenAccept(jobResults -> {
                                try {
                                    CsvUtil.writeCsv(FILENAME, jobResults);
                                } catch (IOException e) {
                                    logger.error("Error writing CSV file: {}", e.getMessage());
                                }
                            })
                            .join();
                })
                .join();

        executor.shutdown();
    }
}
