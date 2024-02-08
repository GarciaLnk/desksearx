package com.garcialnk.desksearx.model;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Class to store the search results. */
public class Result {
  private String filename;
  private String fullPath;
  private String lastModifiedDate;
  private String fileType;

  /** Default constructor. */
  public Result(String filename, String fullPath, FileTime lastModified, String fileType) {
    this.filename = filename;
    this.fullPath = fullPath;
    this.lastModifiedDate = formatFileTime(lastModified);
    this.fileType = fileType;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getFullPath() {
    return fullPath;
  }

  public void setFullPath(String fullPath) {
    this.fullPath = fullPath;
  }

  public String getHomeRelativePath() {
    return fullPath.replace(System.getProperty("user.home"), "~");
  }

  public String getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(FileTime lastModified) {
    this.lastModifiedDate = formatFileTime(lastModified);
  }

  public String getFormattedDate() {
    return lastModifiedDate.substring(0, 10) + " " + lastModifiedDate.substring(11, 19);
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  private String formatFileTime(FileTime fileTime) {
    if (fileTime == null) {
      return "";
    }
    Instant instant = fileTime.toInstant();
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()).format(instant);
  }

  @Override
  public String toString() {
    return "Result{"
        + "filename='"
        + filename
        + '\''
        + ", fullPath='"
        + fullPath
        + '\''
        + ", lastModifiedDate='"
        + lastModifiedDate
        + '\''
        + ", fileType='"
        + fileType
        + '\''
        + '}';
  }
}
