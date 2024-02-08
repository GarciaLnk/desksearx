package com.garcialnk.desksearx.controller;

import static io.github.palexdev.materialfx.validation.Validated.INVALID_PSEUDO_CLASS;

import com.garcialnk.desksearx.model.Settings;
import com.garcialnk.desksearx.service.IndexService;
import com.garcialnk.desksearx.utils.ConfigManager;
import com.garcialnk.desksearx.utils.MoveWindow;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXListCell;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyComboBox;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class represents a controller for the settings functionality. */
public class SettingsController {
  private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
  private static final String DEFAULT_PATH = System.getProperty("user.home");
  private final Stage stage;
  private final Map<String, Boolean> directoryMap = new HashMap<>();
  @FXML private MFXCheckbox indexHiddenCheck;
  @FXML private MFXCheckbox indexContentsCheck;
  @FXML private MFXFontIcon directoryIcon;
  @FXML private Label validationLabel;
  @FXML private MFXListView<String> directoryListView;
  @FXML private MFXButton addButton;
  @FXML private HBox windowHeader;
  @FXML private MFXFontIcon closeIcon;
  @FXML private MFXTextField directoryField;

  public SettingsController(Stage stage) {
    this.stage = stage;
  }

  @FXML
  private void initialize() {
    new MoveWindow(stage, windowHeader);

    Settings settings = ConfigManager.getInstance().getSettings();
    loadSettings(settings);

    closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.close());
    directoryIcon.addEventHandler(
        MouseEvent.MOUSE_CLICKED, event -> handleDirectoryPickerAction(directoryField));
    directoryField
        .delegateFocusedProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (Boolean.TRUE.equals(oldValue && !newValue)) {
                directoryValidation();
              }
            });

    directoryListView.setCellFactory(
        directory -> new DirectoryCellFactory(directoryListView, directory, directoryMap));
  }

  private Boolean directoryValidation() {
    Path path = Paths.get(directoryField.getText());
    String normalizedPath = path.normalize().toAbsolutePath().toString();

    List<String> messages = new ArrayList<>();

    boolean isDirectory = new File(normalizedPath).isDirectory();
    if (!isDirectory) {
      messages.add("Directory does not exist");
    }

    boolean isNotAdded = !directoryMap.containsKey(normalizedPath);
    if (!isNotAdded) {
      messages.add("Directory already added");
    }

    if (!isDirectory || !isNotAdded) {
      directoryField.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
      validationLabel.setText(messages.getFirst());
      validationLabel.setVisible(true);
      validationLabel.setManaged(true);
      addButton.setDisable(true);

      return false;
    } else {
      validationLabel.setVisible(false);
      validationLabel.setManaged(false);
      addButton.setDisable(false);
      directoryField.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);

      return true;
    }
  }

  private void addDirectory(String normalizedPath) {
    directoryMap.put(normalizedPath, true);
    directoryListView.getItems().add(normalizedPath);
  }

  private void deleteDirectory(String normalizedPath) {
    directoryMap.remove(normalizedPath);
    directoryListView.getItems().remove(normalizedPath);
  }

  @FXML
  private void onAddDirectory() {
    Path path = Paths.get(directoryField.getText());
    String normalizedPath = path.normalize().toAbsolutePath().toString();
    if (Boolean.TRUE.equals(directoryValidation())) {
      addDirectory(normalizedPath);
    }
  }

  @FXML
  private void onDeleteDirectory() {
    Path path = Paths.get(directoryField.getText());
    String normalizedPath = path.normalize().toAbsolutePath().toString();
    deleteDirectory(normalizedPath);
    directoryValidation();
  }

  private void loadSettings(Settings settings) {
    if (settings == null) {
      logger.error("Settings is null");
      return;
    }
    List<String> directories = settings.getDirectories();
    MFXListView<String> tempListView = new MFXListView<>();
    directoryMap.clear();
    directoryMap.putAll(directories.stream().collect(Collectors.toMap(dir -> dir, dir -> true)));
    tempListView.getItems().addAll(directories);
    List<String> disabledDirectories = settings.getDisabledDirectories();
    if (disabledDirectories != null && !disabledDirectories.isEmpty()) {
      directoryMap.putAll(
          disabledDirectories.stream().collect(Collectors.toMap(dir -> dir, dir -> false)));
      tempListView.getItems().addAll(disabledDirectories);
    }
    directoryListView.setItems(tempListView.getItems());
    indexHiddenCheck.setSelected(settings.isIndexHidden());
    indexContentsCheck.setSelected(settings.isIndexContents());
  }

  @FXML
  private void onSaveSettings() {
    Settings newSettings = getSettings();
    Settings oldSettings = ConfigManager.getInstance().getSettings();
    ConfigManager.getInstance().saveSettings(newSettings);
    if (!oldSettings.equals(newSettings)) {
      try {
        if (!oldSettings.isIndexContents() && newSettings.isIndexContents()) {
          IndexService.getInstance().refreshIndex(newSettings.getDirectories());
        } else {
          IndexService.getInstance().validateIndex(newSettings.getDirectories());
        }
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
    }
    stage.close();
  }

  private Settings getSettings() {
    Settings newSettings = new Settings();
    List<String> activeDirectories = new ArrayList<>();
    List<String> disabledDirectories = new ArrayList<>();
    for (Map.Entry<String, Boolean> entry : directoryMap.entrySet()) {
      if (Boolean.TRUE.equals(entry.getValue())) {
        activeDirectories.add(entry.getKey());
      } else {
        disabledDirectories.add(entry.getKey());
      }
    }
    newSettings.setDirectories(activeDirectories);
    newSettings.setDisabledDirectories(disabledDirectories);
    newSettings.setIndexHidden(indexHiddenCheck.isSelected());
    newSettings.setIndexContents(indexContentsCheck.isSelected());
    return newSettings;
  }

  @FXML
  private void onDefaultSettings() {
    Settings settings = ConfigManager.getInstance().getSettings();
    settings.resetSettings();
    ConfigManager.getInstance().saveSettings(settings);
    loadSettings(settings);
    try {
      IndexService.getInstance().refreshIndex(settings.getDirectories());
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  @FXML
  private void onCancelSettings() {
    stage.close();
  }

  private void handleDirectoryPickerAction(MFXTextField directoryField) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Select Directory");
    directoryChooser.setInitialDirectory(new File(DEFAULT_PATH));
    File selectedDirectory = directoryChooser.showDialog(null);

    if (selectedDirectory != null) {
      directoryField.setText(selectedDirectory.getAbsolutePath());
      onAddDirectory();
    }
  }

  @FXML
  private void onDirectoryFieldEnter() {
    onAddDirectory();
  }

  private static class DirectoryCellFactory extends MFXListCell<String> {
    private final MFXFontIcon folderIcon;
    private final HBox container;

    public DirectoryCellFactory(
        MFXListView<String> listView, String data, Map<String, Boolean> directoryMap) {
      super(listView, data);
      final String indexed = "Indexed";
      final String notIndexed = "Not Indexed";

      folderIcon = new MFXFontIcon("fas-folder", 16);
      folderIcon.getStyleClass().add("folder-icon");

      MFXLegacyComboBox<String> indexedCombobox = new MFXLegacyComboBox<>();
      indexedCombobox.getItems().addAll(indexed, notIndexed);
      indexedCombobox
          .getSelectionModel()
          .select(
              Boolean.TRUE.equals(directoryMap.getOrDefault(data, true)) ? indexed : notIndexed);
      indexedCombobox
          .valueProperty()
          .addListener(
              (observable, oldValue, newValue) -> directoryMap.put(data, newValue.equals(indexed)));

      MFXFontIcon trashIcon = new MFXFontIcon("fas-trash", 16);
      trashIcon.getStyleClass().add("trash-icon");
      trashIcon.addEventHandler(
          MouseEvent.MOUSE_CLICKED,
          event -> {
            directoryMap.remove(data);
            // create a copy of the list to avoid concurrent modification
            MFXListView<String> tempListView = new MFXListView<>();
            tempListView.getItems().addAll(listView.getItems());
            tempListView.getItems().remove(data);
            listView.setItems(tempListView.getItems());
          });

      setOnMouseClicked(
          event -> {
            MFXTextField inputField = (MFXTextField) listView.getScene().lookup("#directoryField");
            if (inputField != null) {
              inputField.setText(data);
            }
          });

      container = new HBox(10);
      container.setAlignment(Pos.CENTER_RIGHT);
      container.getChildren().addAll(indexedCombobox, trashIcon);

      render(data);
    }

    @Override
    protected void render(String data) {
      super.render(data);
      if (container != null) {
        getChildren().addFirst(folderIcon);
        getChildren().add(container);
        HBox.setHgrow(container, Priority.ALWAYS);
      }
    }
  }
}
