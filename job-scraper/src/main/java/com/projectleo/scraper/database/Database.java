package com.projectleo.scraper.database;

import com.projectleo.scraper.scrapers.JobResult;
import com.projectleo.scraper.util.PropertiesUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Database implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(Database.class);
  private Connection connection;

  public Database() {
    try {
      String jdbcUrl = PropertiesUtil.getProperty("jdbc.url");
      String jdbcUser = PropertiesUtil.getProperty("jdbc.user");
      String jdbcPassword = PropertiesUtil.getProperty("jdbc.password");

      this.connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
    } catch (SQLException e) {
      logger.error("Error initializing database connection: {}", e.getMessage());
      throw new RuntimeException("Error initializing database connection", e);
    }
  }

  public void writeToDatabase(List<JobResult> jobResults) {
    String insertSql =
        "INSERT INTO job_results (company, job_title, industry, responsibilities, qualifications,"
            + " skills, location, job_link_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
      logger.info("Writing {} job results to database", jobResults.size());
      for (JobResult jobResult : jobResults) {
        // No need to check if job link exists in database since it is already checked in the
        // scraper
        insertStmt.setString(1, jobResult.getCompany());
        insertStmt.setString(2, jobResult.getJobTitle());
        insertStmt.setString(3, jobResult.getIndustry());
        insertStmt.setString(4, String.join("|", jobResult.getResponsibilities()));
        insertStmt.setString(5, String.join("|", jobResult.getQualifications()));
        insertStmt.setString(6, String.join("|", jobResult.getSkills()));
        insertStmt.setString(7, jobResult.getLocation());
        insertStmt.setBytes(8, jobResult.getJobLinkHash());
        // TODO: Not setting timestamp in code. Field has CURRENT_TIMESTAMP as default value
        insertStmt.addBatch();
      }
      insertStmt.executeBatch();
      logger.info("Successfully wrote job results to database");
    } catch (SQLException e) {
      logger.error("Error writing to database: {}", e.getMessage());
    }
  }

  public boolean jobLinkExists(byte[] jobLinkHash) {
    String checkSql = "SELECT COUNT(*) FROM job_results WHERE job_link_hash = ?";
    try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
      checkStmt.setBytes(1, jobLinkHash);
      try (ResultSet resultSet = checkStmt.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt(1) > 0;
        }
      }
    } catch (SQLException e) {
      logger.error("Error checking job link hash in database: {}", e.getMessage());
    }
    return false;
  }

  @Override
  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      logger.error("Error closing database connection: {}", e.getMessage());
    }
  }
}
