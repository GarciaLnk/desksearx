package com.garcialnk.desksearx;

import com.garcialnk.desksearx.utils.MoveWindow;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXListCell;
import io.github.palexdev.materialfx.controls.legacy.MFXLegacyComboBox;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.github.palexdev.materialfx.validation.Validated.INVALID_PSEUDO_CLASS;

public class SettingsController {
    private static final String DEFAULT_PATH = System.getProperty("user.home");
    private static final String[] DEFAULT_DIRECTORIES = { DEFAULT_PATH };
    private final Stage stage;

    @FXML
    private MFXCheckbox indexHiddenCheck;
    @FXML
    private MFXCheckbox indexContentsCheck;
    @FXML
    private MFXFontIcon directoryIcon;
    @FXML
    private Label validationLabel;
    @FXML
    private MFXListView<String> directoryList;
    @FXML
    private MFXButton addButton;
    @FXML
    private HBox windowHeader;
    @FXML
    private MFXFontIcon closeIcon;
    @FXML
    private MFXTextField directoryField;

    public SettingsController(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.close());
        directoryIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleDirectoryPickerAction(directoryField));
        directoryField.delegateFocusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(oldValue && !newValue))
                directoryValidation();
        });

        directoryList.setCellFactory(directory -> new DirectoryCellFactory(directoryList, directory));
        directoryList.getItems().addAll(DEFAULT_DIRECTORIES);

        new MoveWindow(stage, windowHeader).moveWindow();
    }

    private Boolean directoryValidation() {
        Path path = Paths.get(directoryField.getText());
        String normalizedPath = path.normalize().toAbsolutePath().toString();

        List<String> messages = new ArrayList<>();

        boolean isDirectory = new File(normalizedPath).isDirectory();
        if (!isDirectory)
            messages.add("Directory does not exist");

        boolean isNotAdded = !directoryList.getItems().contains(normalizedPath);
        if (!isNotAdded)
            messages.add("Directory already added");

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

    @FXML
    private void onAddDirectory() {
        Path path = Paths.get(directoryField.getText());
        String normalizedPath = path.normalize().toAbsolutePath().toString();

        if (Boolean.TRUE.equals(directoryValidation())) {
            directoryList.getItems().add(normalizedPath);
        }
    }

    @FXML
    private void onDeleteDirectory() {
        Path path = Paths.get(directoryField.getText());
        String normalizedPath = path.normalize().toAbsolutePath().toString();

        directoryList.getItems().remove(normalizedPath);
        directoryValidation();
    }

    @FXML
    private void onSaveSettings() {
        throw new UnsupportedOperationException("Not implemented yet");
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
            directoryValidation();
        }
    }

    @FXML
    private void onDirectoryFieldEnter() {
        onAddDirectory();
    }

    @FXML
    private void onDefaultSettings() {
        directoryList.getItems().clear();
        directoryValidation();
        directoryList.getItems().addAll(DEFAULT_DIRECTORIES);
        indexHiddenCheck.setSelected(false);
        indexContentsCheck.setSelected(true);
    }

    private class DirectoryCellFactory extends MFXListCell<String> {
        private final MFXFontIcon folderIcon;
        private final HBox container;

        public DirectoryCellFactory(MFXListView<String> listView, String data) {
            super(listView, data);

            folderIcon = new MFXFontIcon("fas-folder", 16);
            folderIcon.getStyleClass().add("folder-icon");

            MFXLegacyComboBox<String> indexedCombobox = new MFXLegacyComboBox<>();
            indexedCombobox.getItems().addAll("Indexed", "Not Indexed");
            indexedCombobox.getSelectionModel().selectFirst();

            MFXFontIcon trashIcon = new MFXFontIcon("fas-trash", 16);
            trashIcon.getStyleClass().add("trash-icon");
            trashIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                listView.getItems().remove(data);
                directoryValidation();
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
