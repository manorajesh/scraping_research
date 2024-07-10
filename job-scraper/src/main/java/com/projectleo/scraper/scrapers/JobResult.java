package com.projectleo.scraper.scrapers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

public class JobResult {
  @JsonIgnore private MessageDigest jobLinkHash;

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
    this("N/A", "N/A", "N/A", List.of("N/A"), List.of("N/A"), List.of("N/A"), "N/A");
  }

  public JobResult(
      String company,
      String jobTitle,
      String industry,
      List<String> responsibilities,
      List<String> qualifications,
      List<String> skills,
      String location) {
    this.company = company;
    this.jobTitle = jobTitle;
    this.industry = industry;
    this.responsibilities = responsibilities;
    this.qualifications = qualifications;
    this.skills = skills;
    this.location = location;
  }

  // Getters and Setters
  public MessageDigest getJobLinkHash() {
    return jobLinkHash;
  }

  public void setJobLinkHash(MessageDigest jobLinkHash) {
    this.jobLinkHash = jobLinkHash;
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

  // Method to create JobResult from complete JSON
  public static JobResult fromCompleteJson(String jsonStr, double apiCost) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(jsonStr, JobResult.class);
  }

  public String asString() {
    return String.format(
        "%s - %s - %s @ %s: %d Responsibilities, %d Qualifications, %d Skills",
        company,
        jobTitle,
        industry,
        location,
        responsibilities.size(),
        qualifications.size(),
        skills.size());
  }

  public List<String> toCsvRecord() {
    return List.of(
        company,
        jobTitle,
        industry,
        formatAsBulletedList(responsibilities),
        formatAsBulletedList(qualifications),
        formatAsBulletedList(skills),
        location);
  }

  private String formatAsBulletedList(List<String> items) {
    return items.stream().map(item -> "• " + item).collect(Collectors.joining("\n"));
  }
}
