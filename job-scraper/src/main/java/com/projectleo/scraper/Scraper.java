package com.projectleo.scraper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Scraper {
    CompletableFuture<List<String>> fetchJobLinks(String url);
    CompletableFuture<String> fetchJobDetails(String jobLink);
    CompletableFuture<JobResult> parseJobDetails(String jobDetails);
}
