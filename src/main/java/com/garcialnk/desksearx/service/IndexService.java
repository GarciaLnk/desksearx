package com.garcialnk.desksearx.service;

import com.garcialnk.desksearx.model.Result;
import com.garcialnk.desksearx.utils.ConfigManager;
import com.garcialnk.desksearx.utils.FileWatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to manage the index. */
@SuppressWarnings("java:S6548") // Singleton class
public class IndexService implements FileChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(IndexService.class);
  private static final String PATH_FIELD = "path";
  private static final String TYPE_FIELD = "type";
  private static final String CONTENT_FIELD = "content";
  private static IndexService instance;
  private final Directory directory;
  private final Analyzer standardAnalyzer;
  private final Analyzer englishAnalyzer;
  private final Analyzer spanishAnalyzer;
  private final FileWatcher fileWatcher;
  private final ParseService parseService;
  private final ConfigManager configManager;

  private IndexService() throws IOException {
    this.parseService = new ParseService();
    this.standardAnalyzer = new StandardAnalyzer();
    this.englishAnalyzer = new EnglishAnalyzer();
    this.spanishAnalyzer = new SpanishAnalyzer();
    Path indexPath = Path.of(ConfigManager.getConfigDir(), "index");
    this.directory = FSDirectory.open(indexPath);
    this.fileWatcher = new FileWatcher();
    fileWatcher.addFileChangeListener(this);
    this.configManager = ConfigManager.getInstance();
  }

  /** Get the instance of the IndexService. */
  public static synchronized IndexService getInstance() throws IOException {
    if (instance == null) {
      logger.info("Creating new instance of IndexService");
      instance = new IndexService();
    }
    return instance;
  }

  @Override
  public void onFileDeleted(Path filePath) {
    if (!Files.isRegularFile(filePath)) {
      return;
    }
    removeFile(filePath.toString());
  }

  @Override
  public void onFileUpdated(Path filePath) {
    try {
      Thread.sleep(100);
      if (this.configManager.getSettings().isIndexHidden() || !Files.isHidden(filePath)) {
        indexFile(filePath);
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Index a list of directories. */
  public void indexDirectories(List<String> directories) throws IOException {
    if (directories == null) {
      logger.warn("Directories is null");
      directories = ConfigManager.getInstance().getSettings().getDirectories();
    }
    for (String dirPath : directories) {
      try (Stream<Path> stream = Files.list(Path.of(dirPath))) {
        stream
            .filter(
                filePath -> {
                  try {
                    return Files.isRegularFile(filePath)
                        && (ConfigManager.getInstance().getSettings().isIndexHidden()
                            || !Files.isHidden(filePath));
                  } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    return false;
                  }
                })
            .forEach(this::indexFile);
      }
    }
  }

  /** Index a single file. */
  public void indexFile(Path filePath) {
    if (!Files.isRegularFile(filePath)) {
      return;
    }

    String filePathString = filePath.toString();
    String fileContent = "";

    try {
      IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);
      if (ConfigManager.getInstance().getSettings().isIndexContents()) {
        fileContent = parseService.parseToString(filePathString);
        String language = parseService.detectLanguage(fileContent);
        if (language.equals("en")) {
          config = new IndexWriterConfig(englishAnalyzer);
        } else if (language.equals("es")) {
          config = new IndexWriterConfig(spanishAnalyzer);
        }
      }

      try (IndexWriter writer = new IndexWriter(directory, config)) {
        if (fileWatcher.needsReindexing(filePath)) {
          Query query = new TermQuery(new Term(PATH_FIELD, filePathString));
          writer.deleteDocuments(query);

          Document doc = new Document();
          doc.add(new StringField(PATH_FIELD, filePathString, Field.Store.YES));

          String fileType = parseService.detectFile(filePathString);
          doc.add(new StringField(TYPE_FIELD, fileType, Field.Store.YES));

          if (ConfigManager.getInstance().getSettings().isIndexContents()) {
            doc.add(new TextField(CONTENT_FIELD, fileContent, Field.Store.NO));
          }

          writer.addDocument(doc);
          fileWatcher.updateIndexedFileInfo(filePathString, Files.getLastModifiedTime(filePath));
          writer.commit();
        }
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  /** Remove a file from the index. */
  public void removeFile(String filePathString) {
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
      Query query = new TermQuery(new Term(PATH_FIELD, filePathString));
      writer.deleteDocuments(query);
      writer.commit();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  /** Search the index. */
  public List<Result> searchIndex(String queryString) {
    List<Result> results = new ArrayList<>();
    try (DirectoryReader ireader = DirectoryReader.open(directory)) {
      // Add wildcard queries for PATH_FIELD and TYPE_FIELD fields
      BooleanQuery.Builder combinedQueryBuilder = new BooleanQuery.Builder();
      combinedQueryBuilder.add(
          new WildcardQuery(new Term(PATH_FIELD, "*" + queryString + "*")),
          BooleanClause.Occur.SHOULD);
      combinedQueryBuilder.add(
          new WildcardQuery(new Term(TYPE_FIELD, "*" + queryString + "*")),
          BooleanClause.Occur.SHOULD);

      // Add content query if content indexing is enabled
      if (ConfigManager.getInstance().getSettings().isIndexContents()) {
        QueryParser contentParser;
        String language = parseService.detectLanguage(queryString);
        if (language.equals("en")) {
          contentParser = new QueryParser(CONTENT_FIELD, englishAnalyzer);
        } else if (language.equals("es")) {
          contentParser = new QueryParser(CONTENT_FIELD, spanishAnalyzer);
        } else {
          contentParser = new QueryParser(CONTENT_FIELD, standardAnalyzer);
        }
        Query contentQuery = contentParser.parse(queryString);
        combinedQueryBuilder.add(contentQuery, BooleanClause.Occur.SHOULD);
      }

      // Execute search
      IndexSearcher isearcher = new IndexSearcher(ireader);
      BooleanQuery combinedQuery = combinedQueryBuilder.build();
      TopDocs topDocs = isearcher.search(combinedQuery, 100);

      StoredFields storedFields = isearcher.storedFields();
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        Document hitDoc = storedFields.document(scoreDoc.doc);
        String path = hitDoc.get(PATH_FIELD);
        String fileType = hitDoc.get(TYPE_FIELD);
        Path filePath = Path.of(path);
        String filename = filePath.getFileName().toString();
        FileTime lastModified = Files.getLastModifiedTime(filePath);
        Result result = new Result(filename, path, lastModified, fileType);
        results.add(result);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return results;
  }

  /** Validate the index. */
  public void validateIndex(List<String> directories) throws IOException {
    if (directories == null) {
      logger.warn("Directories is null");
      directories = ConfigManager.getInstance().getSettings().getDirectories();
    }
    Map<String, FileTime> indexedFiles = fileWatcher.getIndexedFiles();
    Set<String> indexedFilePaths = new HashSet<>(indexedFiles.keySet());
    Set<String> actualFiles = new HashSet<>();

    for (String dirPath : directories) {
      Path dir = Path.of(dirPath);
      try (Stream<Path> paths = Files.list(dir)) {
        paths
            .filter(
                filePath -> {
                  try {
                    return Files.isRegularFile(filePath)
                        && (ConfigManager.getInstance().getSettings().isIndexHidden()
                            || !Files.isHidden(filePath));
                  } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    return false;
                  }
                })
            .forEach(
                filePath -> {
                  String filePathString = filePath.toString();
                  actualFiles.add(filePathString);
                  try {
                    FileTime fileTime = Files.getLastModifiedTime(filePath);
                    if (!indexedFiles.containsKey(filePathString)
                        || fileTime.toMillis() > indexedFiles.get(filePathString).toMillis()) {
                      indexFile(filePath);
                    }
                  } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                  }
                });
      }
    }

    // Remove entries from the index that should no longer be there
    indexedFilePaths.stream()
        .filter(filePath -> !actualFiles.contains(filePath))
        .forEach(this::removeFile);

    // Start watching for changes in the indexed directories
    fileWatcher.watchDirectories(directories);
  }

  /** Refresh the index. */
  public void refreshIndex(List<String> newDirectories) throws IOException {
    clearIndex();
    indexDirectories(newDirectories);
    fileWatcher.watchDirectories(newDirectories);
  }

  private void clearIndex() throws IOException {
    try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(standardAnalyzer))) {
      writer.deleteAll();
      writer.commit();
    }
    fileWatcher.clearIndexedFiles();
  }

  public void close() throws IOException {
    directory.close();
    fileWatcher.stopWatching();
  }
}
