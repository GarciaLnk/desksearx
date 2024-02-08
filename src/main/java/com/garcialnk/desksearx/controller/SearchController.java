package com.garcialnk.desksearx.controller;

import com.garcialnk.desksearx.model.Result;
import com.garcialnk.desksearx.service.IndexService;
import com.garcialnk.desksearx.utils.MoveWindow;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTableColumn;
import io.github.palexdev.materialfx.controls.MFXTableRow;
import io.github.palexdev.materialfx.controls.MFXTableView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.cell.MFXTableRowCell;
import io.github.palexdev.materialfx.filter.StringFilter;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class represents a controller for the search functionality. */
public class SearchController {
  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
  private final Stage stage;
  @FXML private MFXFontIcon settingsIcon;
  @FXML private MFXFontIcon closeIcon;
  @FXML private MFXFontIcon minimizeIcon;
  @FXML private HBox windowHeader;
  @FXML private AnchorPane rootPane;
  @FXML private MFXTextField searchField;
  @FXML private MFXButton searchButton;
  @FXML private MFXTableView<Result> resultTableView;

  public SearchController(Stage stage) {
    this.stage = stage;
  }

  @FXML
  private void initialize() {
    new MoveWindow(stage, windowHeader, rootPane);

    closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());
    minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> stage.setIconified(true));
    settingsIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> openSettingsWindow());

    setupTable();
    setupSearchField();
  }

  private void setupTable() {
    resultTableView.setTableRowFactory(result -> new ResultRowFactory(resultTableView, result));

    MFXTableColumn<Result> fileNameColumn =
        new MFXTableColumn<>("File Name", true, Comparator.comparing(Result::getFilename));
    fileNameColumn.setPrefWidth(210);

    MFXTableColumn<Result> filePathColumn =
        new MFXTableColumn<>("Path", true, Comparator.comparing(Result::getHomeRelativePath));
    filePathColumn.setPrefWidth(260);

    MFXTableColumn<Result> fileTypeColumn =
        new MFXTableColumn<>("File Type", true, Comparator.comparing(Result::getFileType));
    fileTypeColumn.setPrefWidth(160);

    MFXTableColumn<Result> fileDateColumn =
        new MFXTableColumn<>("Last Modified", true, Comparator.comparing(Result::getFormattedDate));
    fileDateColumn.setPrefWidth(160);

    fileNameColumn.setRowCellFactory(result -> new MFXTableRowCell<>(Result::getFilename));
    filePathColumn.setRowCellFactory(result -> new MFXTableRowCell<>(Result::getHomeRelativePath));
    fileTypeColumn.setRowCellFactory(result -> new MFXTableRowCell<>(Result::getFileType));
    fileDateColumn.setRowCellFactory(result -> new MFXTableRowCell<>(Result::getFormattedDate));

    resultTableView.getTableColumns().add(fileNameColumn);
    resultTableView.getTableColumns().add(filePathColumn);
    resultTableView.getTableColumns().add(fileTypeColumn);
    resultTableView.getTableColumns().add(fileDateColumn);

    resultTableView.getFilters().add(new StringFilter<>("File Type", Result::getFileType));
    resultTableView.getFilters().add(new StringFilter<>("File Name", Result::getFormattedDate));
    resultTableView.getFilters().add(new StringFilter<>("Full Path", Result::getHomeRelativePath));
    resultTableView
        .getFilters()
        .add(new StringFilter<>("Last Modified", Result::getHomeRelativePath));
  }

  private void setupSearchField() {
    Timeline typingDelay = new Timeline(new KeyFrame(Duration.millis(500), e -> performSearch()));
    typingDelay.setCycleCount(1);

    searchField
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              typingDelay.stop();
              typingDelay.playFromStart();
            });
  }

  @FXML
  private void onSearchButtonClick() {
    performSearch();
  }

  @FXML
  private void onSearchFieldEnter() {
    searchButton.fire();
  }

  private void performSearch() {
    try {
      String query = searchField.getText().trim();
      if (!query.isEmpty()) {
        resultTableView.getItems().clear();
        IndexService indexService = IndexService.getInstance();
        List<Result> searchResults = indexService.searchIndex(query);
        for (Result result : searchResults) {
          resultTableView.getItems().add(result);
        }
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private void openSettingsWindow() {
    try {
      Stage settingsStage = new Stage();

      UserAgentBuilder.builder()
          .themes(JavaFXThemes.MODENA)
          .themes(MaterialFXStylesheets.forAssemble(true))
          .setDeploy(true)
          .setResolveAssets(true)
          .build()
          .setGlobal();

      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/settings.fxml"));
      fxmlLoader.setControllerFactory(c -> new SettingsController(settingsStage));

      Scene scene = new Scene(fxmlLoader.load());
      scene.setFill(Color.TRANSPARENT);
      settingsStage.initStyle(StageStyle.TRANSPARENT);
      settingsStage.initModality(Modality.APPLICATION_MODAL);
      settingsStage.setTitle("Settings");
      settingsStage.setMinHeight(400);
      settingsStage.setMinWidth(600);
      settingsStage
          .getIcons()
          .add(
              new Image(
                  Objects.requireNonNull(getClass().getResourceAsStream("/view/logo.png")),
                  64,
                  64,
                  true,
                  true));
      settingsStage.setScene(scene);
      settingsStage.showAndWait();
    } catch (IOException ignored) {
      // Ignore exception
    }
  }

  private static class ResultRowFactory extends MFXTableRow<Result> {
    public ResultRowFactory(MFXTableView<Result> resultTableView, Result result) {
      super(resultTableView, result);
      addEventFilter(
          MouseEvent.MOUSE_CLICKED,
          event -> {
            if (event.getClickCount() == 2) {
              openFile(result.getFullPath());
            }
          });
    }

    private void openFile(String filePath) {
      // Open the file in a new thread to avoid blocking the UI
      if (Desktop.isDesktopSupported()) {
        new Thread(
                () -> {
                  try {
                    Desktop.getDesktop().open(new File(filePath));
                  } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                  }
                })
            .start();
      }
    }
  }
}
