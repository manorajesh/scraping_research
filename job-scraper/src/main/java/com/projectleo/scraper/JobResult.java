package com.projectleo.scraper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;

public class JobResult {
    private String company;
    private String title;
    private String industry;
    private List<String> responsibilities;
    private List<String> qualifications;
    private String location;
    private List<String> other;

    @JsonIgnore
    private double apiCost;

    // Constructors
    public JobResult() {}

    public JobResult(String company, String title, String industry, List<String> responsibilities, List<String> qualifications, String location, List<String> other, double apiCost) {
        this.company = company;
        this.title = title;
        this.industry = industry;
        this.responsibilities = responsibilities;
        this.qualifications = qualifications;
        this.location = location;
        this.other = other;
        this.apiCost = apiCost;
    }

    // Getters and Setters
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public double getApiCost() {
        return apiCost;
    }

    public void setApiCost(double apiCost) {
        this.apiCost = apiCost;
    }

    // Method to create JobResult from partial JSON
    public static JobResult fromImpartialJson(String company, String title, String location, String jsonStr, double apiCost) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PartialJobData data = mapper.readValue(jsonStr, PartialJobData.class);

        return new JobResult(
            company,
            title,
            data.getIndustry(),
            data.getResponsibilities(),
            data.getQualifications(),
            location,
            List.of("N/A"),
            apiCost
        );
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
            company, title, industry, location, responsibilities.size(), qualifications.size(), other.size()
        );
    }

    // Method to convert JobResult to CSV record
    public List<String> toCsvRecord() {
        return List.of(
            company,
            title,
            industry,
            String.join(";", responsibilities),
            String.join(";", qualifications),
            location,
            String.join(";", other)
        );
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
