package com.garcialnk.desksearx.utils;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.garcialnk.desksearx.model.FileIndex;
import com.garcialnk.desksearx.service.FileChangeListener;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to watch for file changes. */
public class FileWatcher {
  private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
  private final ObjectMapper objectMapper;
  private final Map<String, FileTime> indexedFiles;
  private final Path indexedFilesPath = Path.of(ConfigManager.getConfigDir(), "indexedFiles.json");
  private final List<FileChangeListener> listeners = new ArrayList<>();
  private ExecutorService executorService;
  private WatchService watchService;

  public FileWatcher() throws IOException {
    this.objectMapper = new ObjectMapper();
    this.indexedFiles = loadIndexedFilesInfo();
  }

  public void addFileChangeListener(FileChangeListener listener) {
    listeners.add(listener);
  }

  private void initWatcher() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
    this.executorService = Executors.newSingleThreadExecutor();
  }

  /** Stop watching for file changes. */
  public void stopWatching() throws IOException {
    if (watchService != null) {
      watchService.close(); // Close the current watch service
    }
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdownNow(); // Stop the executor service
    }
  }

  /** Watch the given directories for file changes. */
  public void watchDirectories(List<String> directories) throws IOException {
    if (directories == null || directories.isEmpty()) {
      logger.info("No directories to watch");
      return;
    }
    stopWatching();
    initWatcher();

    for (String dirPath : directories) {
      Path dir = Paths.get(dirPath);
      if (Files.isDirectory(dir) && Files.isReadable(dir)) {
        dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
      }
    }

    startWatching();
  }

  private void startWatching() {
    executorService.submit(
        () -> {
          boolean shouldContinue = true;
          try {
            while (!Thread.currentThread().isInterrupted() && shouldContinue) {
              WatchKey key = null;
              try {
                key = watchService.take();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shouldContinue = false;
              } catch (ClosedWatchServiceException e) {
                shouldContinue = false;
              }
              if (shouldContinue && key != null) {
                Path dir = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {
                  WatchEvent.Kind<?> kind = event.kind();
                  @SuppressWarnings("unchecked")
                  WatchEvent<Path> ev = (WatchEvent<Path>) event;
                  Path filename = ev.context();
                  Path child = dir.resolve(filename);

                  if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                    for (FileChangeListener listener : listeners) {
                      if (!Files.isDirectory(child)) {
                        listener.onFileUpdated(child);
                      }
                    }
                  } else if (kind == ENTRY_DELETE) {
                    for (FileChangeListener listener : listeners) {
                      listener.onFileDeleted(child);
                    }
                  }
                }

                boolean valid = key.reset();
                if (!valid) {
                  shouldContinue = false;
                }
              }
            }
          } catch (ClosedWatchServiceException e) {
            logger.error(e.getMessage(), e);
          }
        });
  }

  public Map<String, FileTime> getIndexedFiles() {
    return indexedFiles;
  }

  public void clearIndexedFiles() throws IOException {
    indexedFiles.clear();
    saveIndexedFilesInfo();
  }

  private Map<String, FileTime> loadIndexedFilesInfo() throws IOException {
    if (!Files.exists(indexedFilesPath)) {
      return new HashMap<>();
    }

    CollectionType javaType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, FileIndex.class);
    List<FileIndex> fileIndexList = objectMapper.readValue(indexedFilesPath.toFile(), javaType);

    Map<String, FileTime> fileTimeMap = new HashMap<>();
    for (FileIndex info : fileIndexList) {
      fileTimeMap.put(info.getPath(), FileTime.fromMillis(info.getLastModified()));
    }
    return fileTimeMap;
  }

  /** Check if the file needs to be indexed. */
  public boolean needsReindexing(Path filePath) throws IOException {
    long currentLastModified = Files.getLastModifiedTime(filePath).toMillis();
    if (!indexedFiles.containsKey(filePath.toString())) {
      return true;
    }
    long lastIndexed = indexedFiles.get(filePath.toString()).toMillis();
    return currentLastModified > lastIndexed;
  }

  public void updateIndexedFileInfo(String filePath, FileTime lastModified) throws IOException {
    indexedFiles.put(filePath, lastModified);
    saveIndexedFilesInfo();
  }

  private void saveIndexedFilesInfo() throws IOException {
    List<FileIndex> fileIndexList =
        indexedFiles.entrySet().stream()
            .map(entry -> new FileIndex(entry.getKey(), entry.getValue().toMillis()))
            .toList();
    objectMapper.writeValue(indexedFilesPath.toFile(), fileIndexList);
  }
}
