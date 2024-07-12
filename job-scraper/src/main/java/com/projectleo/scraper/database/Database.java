package com.projectleo.scraper.database;

import com.projectleo.scraper.scrapers.JobResult;
import com.projectleo.scraper.util.PropertiesUtil;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Database implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(Database.class);
  private static final String COLLECTION_NAME = "job_results";
  private static final String DB_URL = PropertiesUtil.getProperty("db.url");
  private static final String DB_USER = PropertiesUtil.getProperty("db.user");
  private static final String DB_PASSWORD = PropertiesUtil.getProperty("db.password");
  private Connection connection;

  public Database() {
    logger.info("Initializing database connection.");
    try {
      connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      createTableIfNotExists();
    } catch (SQLException e) {
      logger.error("Failed to initialize database connection.", e);
      throw new RuntimeException(e);
    }
  }

  private void createTableIfNotExists() throws SQLException {
    String createTableSQL =
        "CREATE TABLE IF NOT EXISTS "
            + COLLECTION_NAME
            + " ("
            + "id SERIAL PRIMARY KEY, "
            + "vector VECTOR(3072), "
            + "job_link_hash BYTEA, "
            + "timestamp BIGINT, "
            + "company TEXT, "
            + "job_title TEXT, "
            + "industry TEXT, "
            + "responsibilities TEXT[], "
            + "qualifications TEXT[], "
            + "skills TEXT[], "
            + "location TEXT"
            + ")";
    try (Statement stmt = connection.createStatement()) {
      stmt.execute(createTableSQL);
      logger.info("Table {} created or already exists.", COLLECTION_NAME);
    }
  }

  public void writeToDatabase(List<JobResult> jobResults) {
    logger.info("Writing {} job results to the database.", jobResults.size());
    String insertSQL =
        "INSERT INTO "
            + COLLECTION_NAME
            + " (vector, job_link_hash, timestamp, company, job_title, industry, responsibilities,"
            + " qualifications, skills, location) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
      for (JobResult jobResult : jobResults) {
        pstmt.setObject(1, jobResult.getVector()); // Storing vector as is
        pstmt.setBytes(2, jobResult.getJobLinkHash());
        pstmt.setLong(3, jobResult.getTimestamp().toEpochSecond(ZoneOffset.UTC));
        pstmt.setString(4, jobResult.getCompany());
        pstmt.setString(5, jobResult.getJobTitle());
        pstmt.setString(6, jobResult.getIndustry());
        pstmt.setArray(
            7,
            connection.createArrayOf(
                "TEXT", jobResult.getResponsibilities().toArray(new String[0])));
        pstmt.setArray(
            8,
            connection.createArrayOf("TEXT", jobResult.getQualifications().toArray(new String[0])));
        pstmt.setArray(
            9, connection.createArrayOf("TEXT", jobResult.getSkills().toArray(new String[0])));
        pstmt.setString(10, jobResult.getLocation());
        pstmt.addBatch();
      }
      pstmt.executeBatch();
      logger.info("Inserted {} records into the database.", jobResults.size());
    } catch (SQLException e) {
      logger.error("Failed to write job results to the database.", e);
      throw new RuntimeException(e);
    }
  }

  public boolean jobLinkExists(byte[] jobLinkHash) {
    logger.info("Checking if job link with hash exists in the database.");
    String searchSQL = "SELECT 1 FROM " + COLLECTION_NAME + " WHERE job_link_hash = ?";

    try (PreparedStatement pstmt = connection.prepareStatement(searchSQL)) {
      pstmt.setBytes(1, jobLinkHash);
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean exists = rs.next();
        logger.info("Job link hash existence check: {}", exists);
        return exists;
      }
    } catch (SQLException e) {
      logger.error("Failed to check job link existence in the database.", e);
      throw new RuntimeException(e);
    }
  }

  public List<Integer> searchByVector(float[] queryVector, int topK) {
    logger.info("Searching for similar vectors in the database.");
    String searchSQL = "SELECT id FROM " + COLLECTION_NAME + " ORDER BY vector <-> ? LIMIT ?";

    try (PreparedStatement pstmt = connection.prepareStatement(searchSQL)) {
      pstmt.setObject(1, queryVector); // Querying vector as is
      pstmt.setInt(2, topK);
      try (ResultSet rs = pstmt.executeQuery()) {
        List<Integer> resultIds = new ArrayList<>();
        while (rs.next()) {
          resultIds.add(rs.getInt("id"));
        }
        logger.info("Found {} similar vectors.", resultIds.size());
        return resultIds;
      }
    } catch (SQLException e) {
      logger.error("Failed to search similar vectors in the database.", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    logger.info("Closing database connection.");
    if (connection != null) {
      try {
        connection.close();
        logger.info("Database connection closed.");
      } catch (SQLException e) {
        logger.error("Failed to close database connection.", e);
        throw new RuntimeException(e);
      }
    }
  }
}
