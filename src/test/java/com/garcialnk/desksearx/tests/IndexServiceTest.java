package com.garcialnk.desksearx.tests;

import static org.junit.jupiter.api.Assertions.*;

import com.garcialnk.desksearx.model.Result;
import com.garcialnk.desksearx.service.IndexService;
import com.garcialnk.desksearx.utils.ConfigManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.lucene.util.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexServiceTest {
  private IndexService indexService;
  private Path testConfigDir;
  private Path testIndexDir;
  private Path testDirectoryPath;

  @BeforeEach
  void setUp() throws IOException, URISyntaxException {
    URL configDirUrl = getClass().getResource("index/config");
    assertNotNull(configDirUrl, "Test config directory not found in resources.");
    testConfigDir = Paths.get(configDirUrl.toURI());
    ConfigManager.setConfigDir(testConfigDir.toString());

    testIndexDir = testConfigDir.resolve("index");
    Files.createDirectories(testIndexDir);

    testDirectoryPath = Files.createTempDirectory(testConfigDir, "testFiles_");

    indexService = IndexService.getInstance();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (Files.exists(testIndexDir)) {
      IOUtils.rm(testIndexDir);
    }

    if (Files.exists(testDirectoryPath)) {
      IOUtils.rm(testDirectoryPath);
    }

    Files.deleteIfExists(testConfigDir.resolve("indexedFiles.json"));
  }

  @Test
  void shouldIndexSingleFile() throws IOException {
    Path filePath = testDirectoryPath.resolve("sample.txt");
    Files.writeString(filePath, "This is a test file content.");

    indexService.indexFile(filePath);

    List<Result> results = indexService.searchIndex("test");
    assertFalse(results.isEmpty(), "Expected to find at least one result matching 'test'");
  }

  @Test
  void shouldRemoveFileFromIndex() throws IOException {
    Path filePath = testDirectoryPath.resolve("toBeRemoved.txt");
    Files.writeString(filePath, "File to be removed from index.");

    indexService.indexFile(filePath);
    indexService.removeFile(filePath.toString());

    List<Result> results = indexService.searchIndex("removed");
    assertTrue(
        results.isEmpty(), "Expected not to find any result matching 'removed' after file removal");
  }

  @Test
  void shouldIndexMultipleDirectories() throws IOException {
    List<String> directories = List.of(testDirectoryPath.toString());
    Path sampleFile1 = testDirectoryPath.resolve("sample1.txt");
    Files.writeString(sampleFile1, "Content for the first sample file.");
    Path sampleFile2 = testDirectoryPath.resolve("sample2.txt");
    Files.writeString(sampleFile2, "Content for the second sample file.");

    indexService.indexDirectories(directories);

    List<Result> results1 = indexService.searchIndex("first");
    List<Result> results2 = indexService.searchIndex("second");

    assertFalse(results1.isEmpty(), "Expected to find at least one result for 'first'");
    assertFalse(results2.isEmpty(), "Expected to find at least one result for 'second'");
  }

  @Test
  void shouldUpdateFileIndex() throws IOException {
    Path filePath = testDirectoryPath.resolve("updateTest.txt");
    Files.writeString(filePath, "Initial content.");
    indexService.indexFile(filePath);

    // Simulate file content update and re-index
    Files.writeString(filePath, "Updated content.");
    indexService.onFileUpdated(filePath);

    List<Result> results = indexService.searchIndex("Updated");
    assertFalse(results.isEmpty(), "Expected to find at least one result matching 'Updated'");
  }

  @Test
  void shouldHandleFileDeletion() throws IOException {
    Path filePath = testDirectoryPath.resolve("deleteTest.txt");
    Files.writeString(filePath, "Content for deletion test.");
    indexService.indexFile(filePath);

    // Simulate file deletion and handle it
    Files.delete(filePath);
    indexService.onFileDeleted(filePath);

    List<Result> results = indexService.searchIndex("deletion");
    assertTrue(
        results.isEmpty(),
        "Expected not to find any result matching 'deletion' after file deletion");
  }

  @Test
  void shouldRefreshIndexWithNewDirectories() throws IOException {
    Path newTestDir = testDirectoryPath.resolve("newDir");
    Files.createDirectories(newTestDir);
    Path newFile = newTestDir.resolve("newFile.txt");
    Files.writeString(newFile, "Content for the new file in new directory.");

    indexService.refreshIndex(List.of(newTestDir.toString()));

    List<Result> results = indexService.searchIndex("new");
    assertFalse(results.isEmpty(), "Expected to find at least one result matching 'new'");
  }

  @Test
  void shouldValidateIndexCorrectly() throws IOException {
    // Setup files and directories for validation
    Path validFile = testDirectoryPath.resolve("validFile.txt");
    Files.writeString(validFile, "Valid file content.");
    indexService.indexFile(validFile);

    Path outdatedFile = testDirectoryPath.resolve("outdatedFile.txt");
    Files.writeString(outdatedFile, "Outdated content.");
    indexService.indexFile(outdatedFile);
    // Simulate content update without reindexing
    Files.writeString(outdatedFile, "Updated outdated content.");

    indexService.validateIndex(List.of(testDirectoryPath.toString()));

    List<Result> results = indexService.searchIndex("Updated");
    assertFalse(
        results.isEmpty(),
        "Expected to find at least one result for updated content in 'outdatedFile.txt'");
  }
}
