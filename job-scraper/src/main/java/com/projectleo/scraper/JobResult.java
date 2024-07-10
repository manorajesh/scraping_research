package com.projectleo.scraper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JobResult {
    @JsonProperty(defaultValue = "N/A")
    private String company;

    @JsonProperty(defaultValue = "N/A")
    private String jobTitle;

    @JsonProperty(defaultValue = "N/A")
    private String industry;

    private List<String> responsibilities;

    private List<String> qualifications;

    @JsonProperty(defaultValue = "N/A")
    private String location;

    private List<String> other;

    // Constructors
    public JobResult() {
        this("N/A", "N/A", "N/A", List.of("N/A"), List.of("N/A"), "N/A", List.of("N/A"));
    }

    public JobResult(String company, String jobTitle, String industry, List<String> responsibilities,
            List<String> qualifications, String location, List<String> other) {
        this.company = company;
        this.jobTitle = jobTitle;
        this.industry = industry;
        this.responsibilities = responsibilities;
        this.qualifications = qualifications;
        this.location = location;
        this.other = other;
    }

    // Getters and Setters
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public List<String> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(List<String> responsibilities) {
        this.responsibilities = responsibilities;
    }

    public List<String> getQualifications() {
        return qualifications;
    }

    public void setQualifications(List<String> qualifications) {
        this.qualifications = qualifications;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getOther() {
        return other;
    }

    public void setOther(List<String> other) {
        this.other = other;
    }

    // Method to create JobResult from partial JSON
    public static JobResult fromImpartialJson(String company, String jobTitle, String location, String jsonStr,
            double apiCost) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PartialJobData data = mapper.readValue(jsonStr, PartialJobData.class);

        return new JobResult(
                company,
                jobTitle,
                data.getIndustry(),
                data.getResponsibilities(),
                data.getQualifications(),
                location,
                List.of("N/A"));
    }

    // Method to create JobResult from complete JSON
    public static JobResult fromCompleteJson(String jsonStr, double apiCost) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonStr, JobResult.class);
    }

    // Method to convert JobResult to string
    public String asString() {
        return String.format(
                "%s - %s - %s @ %s: %d Responsibilities, %d Qualifications, %d Other",
                company, jobTitle, industry, location, responsibilities.size(), qualifications.size(), other.size());
    }

    // Method to convert JobResult to CSV record
    public List<String> toCsvRecord() {
        return List.of(
                company,
                jobTitle,
                industry,
                formatAsBulletedList(responsibilities),
                formatAsBulletedList(qualifications),
                location,
                formatAsBulletedList(other));
    }

    // Helper method to format a list as a bulleted list
    private String formatAsBulletedList(List<String> items) {
        return items.stream()
                .map(item -> "• " + item)
                .collect(Collectors.joining("\n"));
    }

    // Inner class to map partial job data
    private static class PartialJobData {
        private String industry;
        private List<String> responsibilities;
        private List<String> qualifications;

        // Getters and Setters
        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public List<String> getResponsibilities() {
            return responsibilities;
        }

        public void setResponsibilities(List<String> responsibilities) {
            this.responsibilities = responsibilities;
        }

        public List<String> getQualifications() {
            return qualifications;
        }

        public void setQualifications(List<String> qualifications) {
            this.qualifications = qualifications;
        }
    }
}
