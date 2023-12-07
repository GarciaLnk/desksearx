package com.garcialnk.desksearx;

import com.garcialnk.desksearx.utils.MoveWindow;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXListView;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
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

import java.io.IOException;
import java.util.Objects;

public class SearchController {
    private final Stage stage;
    @FXML
    private MFXFontIcon settingsIcon;
    @FXML
    private MFXFontIcon closeIcon;
    @FXML
    private MFXFontIcon minimizeIcon;
    @FXML
    private HBox windowHeader;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private MFXTextField searchField;
    @FXML
    private MFXButton searchButton;
    @FXML
    private MFXListView<String> resultList;

    public SearchController(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> Platform.exit());
        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> ((Stage) rootPane.getScene().getWindow()).setIconified(true));
        settingsIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> openSettingsWindow());

        new MoveWindow(stage, windowHeader).moveWindow();
    }

    @FXML
    private void onSearchButtonClick() {
        resultList.getItems().add(searchField.getText());
    }

    @FXML
    private void onSearchFieldEnter() {
        searchButton.fire();
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

            FXMLLoader fxmlLoader = new FXMLLoader(SearchApplication.class.getResource("settings-view.fxml"));
            fxmlLoader.setControllerFactory(c -> new SettingsController(settingsStage));

            Scene scene = new Scene(fxmlLoader.load());
            scene.setFill(Color.TRANSPARENT);
            settingsStage.initStyle(StageStyle.TRANSPARENT);
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setTitle("Settings");
            settingsStage.minHeightProperty().set(400);
            settingsStage.minWidthProperty().set(600);
            settingsStage.getIcons().add(new Image(Objects.requireNonNull(SearchApplication.class.getResourceAsStream("logo.png")), 64, 64, true, true));
            settingsStage.setScene(scene);
            settingsStage.showAndWait();
        } catch (IOException ignored) {
            // ignore possible exception
        }
    }
}
