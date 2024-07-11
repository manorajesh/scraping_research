package com.projectleo.scraper.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {

  public static List<String> readLines(String filePath) throws IOException {
    return Files.lines(Paths.get(filePath)).collect(Collectors.toList());
  }
}
