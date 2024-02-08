package com.garcialnk.desksearx.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to parse files. */
public class ParseService {
  private static final Logger logger = LoggerFactory.getLogger(ParseService.class);
  private final Tika tika;

  public ParseService() {
    this.tika = new Tika();
    this.tika.setMaxStringLength(-1);
  }

  /** Parse a file to a string. */
  public String parseToString(String filePath) {
    Path path = Paths.get(filePath);
    try {
      if (!Files.exists(path)) {
        logger.warn("File not found: {}", filePath);
        return "";
      }
      return tika.parseToString(path);
    } catch (IOException | TikaException e) {
      logger.warn(e.getMessage(), e);
      return "";
    }
  }

  /** Detect the file type. */
  public String detectFile(String filePath) {
    Path path = Paths.get(filePath);
    try {
      if (!Files.exists(path)) {
        logger.warn("File not found: {}", filePath);
        return "";
      }
      return tika.detect(path);
    } catch (IOException e) {
      logger.warn(e.getMessage(), e);
      return "";
    }
  }

  /** Detect the file language. */
  public String detectLanguage(String text) throws IOException {
    LanguageDetector detector = new OptimaizeLangDetector().loadModels(Set.of("en", "es"));
    LanguageResult result = detector.detect(text);
    if (result.getRawScore() < 0.5) {
      return "";
    }
    return result.getLanguage();
  }
}
