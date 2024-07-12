package com.projectleo.scraper.scrapers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class JobResult {
  @JsonIgnore private double[] vector;

  @JsonIgnore private byte[] jobLinkHash;

  @JsonIgnore private LocalDateTime timestamp;

  @JsonProperty(defaultValue = "N/A")
  private String company;

  @JsonProperty(defaultValue = "N/A")
  private String jobTitle;

  @JsonProperty(defaultValue = "N/A")
  private String industry;

  private List<String> responsibilities;

  private List<String> qualifications;

  private List<String> skills;

  @JsonProperty(defaultValue = "N/A")
  private String location;

  // Constructors
  public JobResult() {
    this(
        "N/A",
        "N/A",
        "N/A",
        List.of("N/A"),
        List.of("N/A"),
        List.of("N/A"),
        "N/A",
        LocalDateTime.now());
  }

  public JobResult(
      String company,
      String jobTitle,
      String industry,
      List<String> responsibilities,
      List<String> qualifications,
      List<String> skills,
      String location,
      LocalDateTime timestamp) {
    this.company = company;
    this.jobTitle = jobTitle;
    this.industry = industry;
    this.responsibilities = responsibilities;
    this.qualifications = qualifications;
    this.skills = skills;
    this.location = location;
    this.timestamp = timestamp;
  }

  // Getters and Setters
  public double[] getVector() {
    return vector;
  }

  public void setVector(double[] vector) {
    this.vector = vector;
  }

  public byte[] getJobLinkHash() {
    if (jobLinkHash == null) {
      throw new IllegalStateException("Job link hash has not been set");
    }
    return jobLinkHash;
  }

  public void setJobLinkHash(String jobLink) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      this.jobLinkHash = digest.digest(jobLink.getBytes());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

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

  public List<String> getSkills() {
    return skills;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  // Method to create JobResult from complete JSON
  public static JobResult fromCompleteJson(String jsonStr) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(jsonStr, JobResult.class);
  }

  public String asString() {
    return String.format(
        "%s - %s - %s @ %s: %d Responsibilities, %d Qualifications, %d Skills, Timestamp: %s",
        company,
        jobTitle,
        industry,
        location,
        responsibilities.size(),
        qualifications.size(),
        skills.size(),
        timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  public List<String> toCsvRecord() {
    return List.of(
        company,
        jobTitle,
        industry,
        formatAsBulletedList(responsibilities),
        formatAsBulletedList(qualifications),
        formatAsBulletedList(skills),
        location,
        timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }

  private String formatAsBulletedList(List<String> items) {
    return items.stream().map(item -> "• " + item).collect(Collectors.joining("\n"));
  }
}
