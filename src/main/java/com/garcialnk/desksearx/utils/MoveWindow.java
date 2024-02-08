package com.garcialnk.desksearx.utils;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** This class represents a utility to move the window. */
public class MoveWindow {
  private final Stage stage;
  private final HBox windowHeader;
  private AnchorPane rootPane;
  private double offsetX;
  private double offsetY;
  private boolean resize = false;

  /** Constructor to move the window. */
  public MoveWindow(Stage stage, HBox windowHeader) {
    this.stage = stage;
    this.windowHeader = windowHeader;
    this.moveWindow();
  }

  /** Constructor to move and resize the window. */
  public MoveWindow(Stage stage, HBox windowHeader, AnchorPane rootPane) {
    this.stage = stage;
    this.windowHeader = windowHeader;
    this.rootPane = rootPane;
    this.moveWindow();
    this.resizeWindow();
  }

  /** Moves the window. */
  private void moveWindow() {
    this.windowHeader.setOnMousePressed(
        event -> {
          this.offsetX = event.getSceneX();
          this.offsetY = event.getSceneY();
          resize = false;
        });
    this.windowHeader.setOnMouseDragged(
        event -> {
          if (!resize) {
            stage.setX(event.getScreenX() - this.offsetX);
            stage.setY(event.getScreenY() - this.offsetY);
          }
        });
  }

  /** Resizes the window. */
  private void resizeWindow() {
    this.rootPane.setOnMousePressed(
        event -> {
          if (event.getX() > stage.getWidth() - 10
              && event.getX() < stage.getWidth() + 10
              && event.getY() > stage.getHeight() - 10
              && event.getY() < stage.getHeight() + 10) {
            this.offsetX = stage.getWidth() - event.getX();
            this.offsetY = stage.getHeight() - event.getY();
            resize = true;
          }
        });
    this.rootPane.setOnMouseDragged(
        event -> {
          if (resize) {
            stage.setWidth(Math.max(event.getX() + this.offsetX, stage.getMinWidth()));
            stage.setHeight(Math.max(event.getY() + this.offsetY, stage.getMinHeight()));
          }
        });
  }
}
