package com.garcialnk.desksearx.service;

import java.nio.file.Path;

/** Interface to listen for file changes. */
public interface FileChangeListener {

  void onFileDeleted(Path filePath);

  void onFileUpdated(Path filePath);
}
