package com.garcialnk.desksearx.utils;

import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MoveWindow {
    private final Stage stage;
    private final HBox windowHeader;
    private double xOffset;
    private double yOffset;

    private MoveWindow() {
        throw new IllegalStateException("Utility class");
    }

    public MoveWindow(Stage stage, HBox windowHeader) {
        this.stage = stage;
        this.windowHeader = windowHeader;
    }

    public void moveWindow() {
        this.windowHeader.setOnMousePressed(event -> {
            this.xOffset = stage.getX() - event.getScreenX();
            this.yOffset = stage.getY() - event.getScreenY();
        });
        this.windowHeader.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + this.xOffset);
            stage.setY(event.getScreenY() + this.yOffset);
        });
    }
}
