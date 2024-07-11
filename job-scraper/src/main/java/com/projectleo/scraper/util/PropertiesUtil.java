package com.projectleo.scraper.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesUtil {
  private static final String PROPERTIES_FILE = "config.properties";
  private static Properties properties = new Properties();

  static {
    try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE)) {
      properties.load(fis);
    } catch (IOException e) {
      throw new RuntimeException("Error loading properties file", e);
    }
  }

  public static String getProperty(String key) {
    return properties.getProperty(key);
  }
}
