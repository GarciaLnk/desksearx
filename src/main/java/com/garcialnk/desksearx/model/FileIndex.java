package com.garcialnk.desksearx.model;

/** Class to store the path and last modified date of an index entry. */
public class FileIndex {
  private String path;
  private long lastModified;

  public FileIndex() {}

  public FileIndex(String path, long lastModified) {
    this.path = path;
    this.lastModified = lastModified;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getLastModified() {
    return lastModified;
  }

  public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
  }
}
