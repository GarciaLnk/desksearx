package com.garcialnk.desksearx.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garcialnk.desksearx.model.Settings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to manage the configuration. */
@SuppressWarnings("java:S6548") // Singleton class
public class ConfigManager {
  private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
  private static String configDir = System.getProperty("user.home") + File.separator + ".desksearx";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Settings settings;

  private ConfigManager() {
    loadSettings();
  }

  public static ConfigManager getInstance() {
    return Holder.INSTANCE;
  }

  public static String getConfigDir() {
    return configDir;
  }

  public static void setConfigDir(String configDir) {
    ConfigManager.configDir = configDir;
  }

  private void loadSettings() {
    try {
      File configFile = new File(configDir, "config.json");
      if (configFile.exists()) {
        settings = objectMapper.readValue(configFile, Settings.class);
      } else {
        settings = new Settings(); // Initialize with default settings
      }
    } catch (IOException e) {
      settings = new Settings();
    }
  }

  /** Save the settings to the config file. */
  public void saveSettings(Settings newSettings) {
    this.settings = newSettings;
    try {
      Path configPath = Paths.get(configDir);
      Files.createDirectories(configPath);
      objectMapper.writeValue(configPath.resolve("config.json").toFile(), newSettings);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public Settings getSettings() {
    return settings;
  }

  private static class Holder {
    static final ConfigManager INSTANCE = new ConfigManager();
  }
}
