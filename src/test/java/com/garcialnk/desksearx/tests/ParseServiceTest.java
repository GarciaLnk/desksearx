package com.garcialnk.desksearx.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.garcialnk.desksearx.service.ParseService;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParseServiceTest {
  private ParseService parseService;
  private String pdfPath;
  private String odtPath;

  @BeforeEach
  void setUp() throws URISyntaxException {
    URL pdfUrl = getClass().getResource("parse/test.pdf");
    assertNotNull(pdfUrl, "Test PDF file not found in resources.");
    pdfPath = Paths.get(pdfUrl.toURI()).toString();

    URL odtUrl = getClass().getResource("parse/test.odt");
    assertNotNull(odtUrl, "Test ODT file not found in resources.");
    odtPath = Paths.get(odtUrl.toURI()).toString();

    parseService = new ParseService();
  }

  @Test
  void testParseToString() {
    String resultPdf = parseService.parseToString(pdfPath).trim();
    String resultOdt = parseService.parseToString(odtPath).trim();

    assertEquals("This is a test PDF file.", resultPdf);
    assertEquals("This is a test ODT file.", resultOdt);
  }

  @Test
  void testDetectFile() {
    String pdfType = parseService.detectFile(pdfPath);
    String odtType = parseService.detectFile(odtPath);

    assertEquals("application/pdf", pdfType);
    assertEquals("application/vnd.oasis.opendocument.text", odtType);
  }

  @Test
  void testDetectLanguage() throws IOException {
    String englishText = "This is an English text sample for language detection.";
    String enLanguage = parseService.detectLanguage(englishText);
    String spanishText = "Este es un ejemplo de texto en español para la detección de idioma.";
    String esLanguage = parseService.detectLanguage(spanishText);

    assertEquals("en", enLanguage);
    assertEquals("es", esLanguage);
  }
}
