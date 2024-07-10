package com.projectleo.scraper;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvUtil {
    private static final Logger logger = LogManager.getLogger(CsvUtil.class);

    public static void writeCsv(String filename, List<JobResult> jobResults) throws IOException {
        logger.info("Writing {} job results to CSV file: {}", jobResults.size(), filename);

        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            for (JobResult jobResult : jobResults) {
                writer.writeNext(jobResult.toCsvRecord().toArray(new String[0]));
            }
        } catch (IOException e) {
            logger.error("Error writing to CSV file: {}", filename, e);
            throw e;
        }

        logger.info("Successfully wrote job results to CSV file: {}", filename);
    }
}
