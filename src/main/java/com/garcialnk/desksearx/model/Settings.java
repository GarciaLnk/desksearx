package com.garcialnk.desksearx.model;

import java.util.List;
import java.util.Objects;

/** Class to store the settings. */
public class Settings {
  private final List<String> defaultDirectories = List.of(System.getProperty("user.home"));
  private boolean indexHidden;
  private boolean indexContents;
  private List<String> directories;
  private List<String> disabledDirectories;

  /** Default constructor. */
  public Settings() {
    this.indexHidden = false;
    this.indexContents = true;
    this.directories = defaultDirectories;
  }

  public List<String> getDisabledDirectories() {
    return disabledDirectories;
  }

  public void setDisabledDirectories(List<String> disabledDirectories) {
    this.disabledDirectories = disabledDirectories;
  }

  public List<String> getDefaultDirectories() {
    return defaultDirectories;
  }

  public boolean isIndexHidden() {
    return indexHidden;
  }

  public void setIndexHidden(boolean indexHidden) {
    this.indexHidden = indexHidden;
  }

  public boolean isIndexContents() {
    return indexContents;
  }

  public void setIndexContents(boolean indexContents) {
    this.indexContents = indexContents;
  }

  public List<String> getDirectories() {
    return directories;
  }

  public void setDirectories(List<String> directories) {
    this.directories = directories;
  }

  /** Reset the settings to the default values. */
  public void resetSettings() {
    this.indexHidden = false;
    this.indexContents = true;
    this.directories = defaultDirectories;
    this.disabledDirectories.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Settings settings)) {
      return false;
    }
    return isIndexHidden() == settings.isIndexHidden()
        && isIndexContents() == settings.isIndexContents()
        && getDirectories().equals(settings.getDirectories());
  }

  @Override
  public int hashCode() {
    return Objects.hash(isIndexHidden(), isIndexContents(), getDirectories());
  }
}
