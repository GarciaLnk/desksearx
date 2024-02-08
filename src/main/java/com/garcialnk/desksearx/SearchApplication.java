package com.garcialnk.desksearx;

import com.garcialnk.desksearx.controller.SearchController;
import com.garcialnk.desksearx.service.IndexService;
import com.garcialnk.desksearx.utils.ConfigManager;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import java.io.IOException;
import java.util.Objects;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the application. */
public class SearchApplication extends Application {
  private static final Logger logger = LoggerFactory.getLogger(SearchApplication.class);

  /** Main method to launch the application. */
  public static void main(String[] args) {
    System.setProperty(
        "prism.forceGPU",
        "true"); // https://github.com/gluonhq/substrate/issues/891#issuecomment-802169710
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    // Create and display the loading screen
    Stage loadingStage = createLoadingStage();
    loadingStage.show();

    // Load the main application in a background task
    Task<Void> loadTask =
        new Task<>() {
          @Override
          protected Void call() throws Exception {
            ConfigManager configManager = ConfigManager.getInstance();
            IndexService indexService = IndexService.getInstance();
            indexService.validateIndex(configManager.getSettings().getDirectories());
            return null;
          }
        };

    loadTask.setOnSucceeded(
        event -> {
          loadingStage.close();
          try {
            showMainStage(primaryStage);
          } catch (IOException e) {
            logger.error("Failed to show main stage", e);
          }
        });

    loadTask.setOnFailed(
        event -> {
          logger.error("Failed to load index", loadTask.getException());
          loadingStage.close();
        });

    new Thread(loadTask).start();
  }

  private Stage createLoadingStage() {
    Stage stage = new Stage(StageStyle.TRANSPARENT);
    ProgressIndicator progressIndicator = new ProgressIndicator();
    Label loadingLabel = new Label("Validating index, please wait...");
    loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
    VBox rootPane = new VBox(20, progressIndicator, loadingLabel);
    rootPane.setStyle(
        "-fx-background-color: rgba(255, 255, 255, 0.8); -fx-padding: 20; -fx-alignment: center;");
    Scene scene = new Scene(rootPane, 300, 200);
    stage.setTitle("DeskSearx");
    stage
        .getIcons()
        .add(
            new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/view/logo.png")),
                64,
                64,
                true,
                true));
    stage.setScene(scene);
    stage.show();
    return stage;
  }

  private void showMainStage(Stage stage) throws IOException {
    if (stage == null) {
      logger.error("Stage is null");
      return;
    }
    CSSFX.start();

    UserAgentBuilder.builder()
        .themes(JavaFXThemes.MODENA)
        .themes(MaterialFXStylesheets.forAssemble(true))
        .setDeploy(true)
        .setResolveAssets(true)
        .build()
        .setGlobal();

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/search.fxml"));
    fxmlLoader.setControllerFactory(c -> new SearchController(stage));

    Scene scene = new Scene(fxmlLoader.load());
    scene.setFill(Color.TRANSPARENT);
    stage.initStyle(StageStyle.TRANSPARENT);
    stage.setTitle("DeskSearx");
    stage.minHeightProperty().set(400);
    stage.minWidthProperty().set(600);
    stage
        .getIcons()
        .add(
            new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/view/logo.png")),
                64,
                64,
                true,
                true));
    stage.setScene(scene);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    IndexService.getInstance().close();
    super.stop();
  }
}
